package ucles.weblab.files.webapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ucles.weblab.files.domain.SecureFileMetadataEntity;
import ucles.weblab.files.domain.SecureFileMetadataRepository;
import ucles.weblab.common.webapi.AccessAudited;
import ucles.weblab.common.webapi.exception.ResourceNotFoundException;
import ucles.weblab.files.domain.FilesFactory;
import ucles.weblab.files.domain.SecureFile;
import ucles.weblab.files.domain.SecureFileCollection;
import ucles.weblab.files.domain.SecureFileCollectionEntity;
import ucles.weblab.files.domain.SecureFileCollectionRepository;
import ucles.weblab.files.domain.SecureFileEntity;
import ucles.weblab.files.domain.SecureFileRepository;
import ucles.weblab.files.webapi.converter.FileCollectionResourceAssembler;
import ucles.weblab.files.webapi.converter.FileMetadataResourceAssembler;
import ucles.weblab.files.webapi.resource.FileCollectionResource;
import ucles.weblab.files.webapi.resource.FileMetadataResource;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static ucles.weblab.common.webapi.HateoasUtils.locationHeader;
import static ucles.weblab.common.webapi.LinkRelation.SELF;

/**
 * Web API for dealing with secure files.
 *
 * @since 19/03/15
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final String IS_AUTHENTICATED = "isAuthenticated()";
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final FilesFactory filesFactory;
    private final SecureFileCollectionRepository secureFileCollectionRepository;
    private final SecureFileRepository secureFileRepository;
    private final SecureFileMetadataRepository secureFileMetadataRepository;
    private final FileMetadataResourceAssembler fileMetadataResourceAssembler;
    private final FileCollectionResourceAssembler fileCollectionResourceAssembler;
    private final DownloadController downloadController;
    private final Supplier<SecureFileCollection.Builder> secureFileCollectionBuilder;
    private final Supplier<SecureFile.Builder> secureFileBuilder;
    private final FileDownloadCache<UUID, PendingDownload> downloadCache;
    private Clock clock = Clock.systemUTC();

    private final Object mutex = new Object();

    private static class DecryptionFailedException extends NestedRuntimeException {
        DecryptionFailedException(Exception e) {
            super("Failed to decrypt file data", e);
        }
    }

    @Autowired
    public FileController(FilesFactory filesFactory,
                          SecureFileCollectionRepository secureFileCollectionRepository,
                          SecureFileRepository secureFileRepository,
                          SecureFileMetadataRepository secureFileMetadataRepository,
                          FileMetadataResourceAssembler fileMetadataResourceAssembler,
                          FileCollectionResourceAssembler fileCollectionResourceAssembler,
                          DownloadController downloadController,
                          Supplier<SecureFileCollection.Builder> secureFileCollectionBuilder,
                          Supplier<SecureFile.Builder> secureFileBuilder,
                          FileDownloadCache<UUID, PendingDownload> downloadCache) {
        this.filesFactory = filesFactory;
        this.secureFileCollectionRepository = secureFileCollectionRepository;
        this.secureFileRepository = secureFileRepository;
        this.secureFileMetadataRepository = secureFileMetadataRepository;
        this.fileMetadataResourceAssembler = fileMetadataResourceAssembler;
        this.fileCollectionResourceAssembler = fileCollectionResourceAssembler;
        this.downloadController = downloadController;
        this.secureFileCollectionBuilder = secureFileCollectionBuilder;
        this.secureFileBuilder = secureFileBuilder;
        this.downloadCache = downloadCache;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = APPLICATION_JSON_UTF8_VALUE)
    @PreAuthorize(IS_AUTHENTICATED)
    public List<FileCollectionResource> listBuckets() {
        return secureFileCollectionRepository.findAll().stream()
                .map(fileCollectionResourceAssembler::toResource)
                .collect(toList());
    }

    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<FileCollectionResource> saveBucket(@RequestBody FileCollectionResource newBucket) {
        SecureFileCollection collectionDefinition = secureFileCollectionBuilder.get()
                .displayName(newBucket.getDisplayName())
                .purgeInstant(Optional.ofNullable(newBucket.getPurgeInstant()))
                .get();
        final SecureFileCollectionEntity savedCollection = secureFileCollectionRepository.save(filesFactory.newSecureFileCollection(collectionDefinition));
        final FileCollectionResource resource = fileCollectionResourceAssembler.toResource(savedCollection);
        return new ResponseEntity<>(resource, locationHeader(resource), HttpStatus.CREATED);
    }

    @RequestMapping(value = "/{bucket}/", method = RequestMethod.GET, produces = APPLICATION_JSON_UTF8_VALUE)
    @AccessAudited
    public ResponseEntity<List<FileMetadataResource>> listFilesInBucket(@PathVariable String bucket) {
        SecureFileCollectionEntity collection = secureFileCollectionRepository.findOneByBucket(bucket);
        if (collection == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(secureFileMetadataRepository.findAllByCollection(collection).stream()
                .map(fileMetadataResourceAssembler::toResource)
                .collect(toList()));
    }

    @RequestMapping(value = "/{bucket}/{filename}/", method = RequestMethod.GET, produces = APPLICATION_JSON_UTF8_VALUE)
    @AccessAudited
    public ResponseEntity<FileMetadataResource> getFileMetadata(@PathVariable String bucket, @PathVariable String filename) {
        SecureFileCollectionEntity collection = secureFileCollectionRepository.findOneByBucket(bucket);
        if (collection == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        final Optional<? extends SecureFileMetadataEntity> found = secureFileMetadataRepository.findOneByCollectionAndFilename(collection, filename);
        return found
                .map(secureFile -> ResponseEntity.ok(fileMetadataResourceAssembler.toResource(secureFile)))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(value = "/{bucket}/{filename}/", method = RequestMethod.PUT, consumes = APPLICATION_JSON_UTF8_VALUE, produces = APPLICATION_JSON_UTF8_VALUE)
    @AccessAudited
    public FileMetadataResource updateFileMetadata(@PathVariable String bucket, @PathVariable String filename, @RequestBody FileMetadataResource update) {
        SecureFileCollectionEntity collection = secureFileCollectionRepository.findOneByBucket(bucket);
        if (collection == null) {
            throw new ResourceNotFoundException(bucket);
        }
        final SecureFileEntity file = secureFileRepository.findOneByCollectionAndFilename(collection, filename)
                .orElseThrow(() -> new ResourceNotFoundException(filename));

        if (update.getContentType() != null) {
            file.setContentType(update.getContentType());
        }
        if (update.getFilename() != null) {
            file.setFilename(update.getFilename());
        }
        if (update.getNotes() != null) {
            file.setNotes(update.getNotes());
        }
        return fileMetadataResourceAssembler.toResource(secureFileRepository.save(file));
    }

    @RequestMapping(value = "/{bucket}/{filename}/", method = RequestMethod.DELETE, produces = APPLICATION_JSON_UTF8_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @AccessAudited
    public void deleteUploadedFile(@PathVariable String bucket, @PathVariable String filename) {
        SecureFileCollectionEntity collection = secureFileCollectionRepository.findOneByBucket(bucket);
        if (collection == null) {
            throw new ResourceNotFoundException(bucket);
        }
        final SecureFileEntity file = secureFileRepository.findOneByCollectionAndFilename(collection, filename)
                .orElseThrow(() -> new ResourceNotFoundException(filename));

        secureFileRepository.delete(file);
    }

    @RequestMapping(value = "/{bucket}/{filename}/preview/", method = RequestMethod.GET)
    @AccessAudited
    public ResponseEntity<byte[]> fetchFileContent(@PathVariable String bucket, @PathVariable String filename) {
        final SecureFileCollectionEntity collection = secureFileCollectionRepository.findOneByBucket(bucket);
        if (collection == null) {
            throw new ResourceNotFoundException(bucket);
        }
        final SecureFileEntity file = secureFileRepository.findOneByCollectionAndFilename(collection, filename)
            .orElseThrow(() -> new ResourceNotFoundException(filename));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(file.getContentType()));
        return new ResponseEntity<>(file.getPlainData(), headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/{bucket}/{filename}/download/", method = RequestMethod.GET)
    @PreAuthorize(IS_AUTHENTICATED)
    @AccessAudited
    public ResponseEntity<ResourceSupport> redirectToDownload(@PathVariable String bucket, @PathVariable String filename) {
        return redirectToDownload(bucket, filename, SecureFile::getPlainData);
    }

    @RequestMapping(value = "/{bucket}/{filename}/downloadEncrypted/", method = RequestMethod.GET)
    @PreAuthorize(IS_AUTHENTICATED)
    @AccessAudited
    public ResponseEntity<ResourceSupport> redirectToDownloadEncrypted(@PathVariable String bucket, @PathVariable String filename) {
        return redirectToDownload(bucket, filename, SecureFile::getEncryptedData);
    }

    private ResponseEntity<ResourceSupport> redirectToDownload(@PathVariable String bucket, @PathVariable String filename, Function<SecureFile, byte[]> extractData) {
        final SecureFileCollectionEntity collection = secureFileCollectionRepository.findOneByBucket(bucket);
        if (collection == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        final Optional<? extends SecureFileEntity> found = secureFileRepository.findOneByCollectionAndFilename(collection, filename);
        return found.map((secureFile) -> {
            try {

                UUID id = UUID.randomUUID();
                //get the url
                Optional<URI> res = downloadCache.getUrl(id, bucket, secureFile.getFilename());

                //create PendingDownload to save
                PendingDownload pd = new PendingDownload(MediaType.valueOf(secureFile.getContentType()),
                        secureFile.getFilename(),
                        extractData.apply(secureFile),
                        Instant.now(clock).plus(this.downloadCache.getExpiry()),
                        res.orElse(null));

                //put it in the cache
                downloadCache.put(id, bucket, pd);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.valueOf(secureFile.getContentType()));
                headers.setLocation(downloadController.generateDownload(id, bucket, secureFile));
                final ResourceSupport resource = new ResourceSupport();
                resource.add(new Link(headers.getLocation().toASCIIString(), SELF.rel()));
                return new ResponseEntity<>(resource, headers, HttpStatus.SEE_OTHER);
            } catch (TransientDataAccessResourceException e) {
                throw new DecryptionFailedException(e);
            }
        }).orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    /**
     * The first point of call when the user clicks on the download.
     */
    @RequestMapping(value = "/{bucket}/{filename}/download/",
                    method = RequestMethod.POST,
                    produces = APPLICATION_JSON_UTF8_VALUE)
    @PreAuthorize(IS_AUTHENTICATED)
    @AccessAudited
    public ResponseEntity<ResourceSupport> generateDownloadLink(@PathVariable String bucket,
                                                                @PathVariable String filename) {
        return generateDownloadLink(bucket, filename, false);
    }

    /**
     * The first point of call when the user clicks on the download encrypted file.
     */
    @RequestMapping(value = "/{bucket}/{filename}/downloadEncrypted/",
            method = RequestMethod.POST,
            produces = APPLICATION_JSON_UTF8_VALUE)
    @PreAuthorize(IS_AUTHENTICATED)
    @AccessAudited
    @CrossOrigin
    public ResponseEntity<ResourceSupport> generateEncryptedDownloadLink(@PathVariable String bucket,
                                                                         @PathVariable String filename) {
        return generateDownloadLink(bucket, filename, true);
    }

    private ResponseEntity<ResourceSupport> generateDownloadLink(@PathVariable String bucket, @PathVariable String filename, boolean encrypted) {
        //get the collection name
        final SecureFileCollectionEntity collection = secureFileCollectionRepository.findOneByBucket(bucket);
        if (collection == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        //always create a random id, first time round
        UUID id = UUID.randomUUID();
        URI location;
        Optional<PendingDownload> fileOpt;

        synchronized (mutex) {
            //one thread at a time
            log.info(Thread.currentThread().getName() + " thread is getting {} from cache", filename);
            fileOpt = downloadCache.get(id, bucket, filename);

            if (fileOpt.isPresent()) {
                log.info(Thread.currentThread().getName() + " thread found {} in cache", filename);
                //if it's there, then set the location
                location = fileOpt.get().getUrl();
            } else {
                log.info(Thread.currentThread().getName() + " thread not found {} in cache, going to database", filename);
                //this should only be done ONCE no matter how many first threads.
                Optional<URI> optReturn  = getLocation(id, bucket, collection, filename, encrypted);
                if (optReturn.isPresent()) {
                    location = optReturn.get();
                } else {
                    log.info("{} not found in repository, returning 404", filename);
                    //it's not in the cache nor the repository
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }
            }
        }

        return getRedirectResponseOrNotFound(location);
    }

    /**
     * Private helper method to get the location of a SecureFileEntity
     * @param id - the id used for the download
     * @param bucket - the collection name
     * @param collection - the collection itself
     * @param filename - the file to get
     * @param isEncrypted - whether or not to return encrypted file data
     */
    private Optional<URI> getLocation(UUID id, String bucket, SecureFileCollectionEntity collection, String filename, boolean isEncrypted) {
        synchronized (this) {
            final Optional<? extends SecureFileEntity> found = secureFileRepository.findOneByCollectionAndFilename(collection, filename);

            //if there is a filename
            if (found.isPresent()) {
                log.info(Thread.currentThread().getName() + " thread getting file from database and adding cache");
                //get the file object
                SecureFileEntity secureFile = found.get();

                //each cache knows how to create a url
                Optional<URI> res = downloadCache.getUrl(id, bucket, secureFile.getFilename());

                //create PendingDownload to put in the cache
                PendingDownload pd = new PendingDownload(MediaType.valueOf(secureFile.getContentType()),
                        secureFile.getFilename(),
                        isEncrypted ? secureFile.getEncryptedData() : secureFile.getPlainData(),
                        Instant.now(clock).plus(this.downloadCache.getExpiry()),
                        res.orElse(null));
                //put it in the cache
                downloadCache.put(id, bucket, pd);

                //get location
                URI location = downloadController.generateDownload(id, bucket, secureFile);

                return Optional.of(location);
            } else {
                return Optional.empty();
            }
        }
    }

    @RequestMapping(value = "/", method = RequestMethod.POST, consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<FileMetadataResource> uploadFileToBucket(@RequestParam String collection, @RequestParam(required = false) String notes, @RequestParam MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        SecureFileCollectionEntity bucket = secureFileCollectionRepository.findOneByDisplayName(collection);
        if (bucket == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        final SecureFile fileDefinition = secureFileBuilder.get()
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .length(file.getSize())
                .plainData(file.getBytes())
                .notes(notes)
                .get();
        final SecureFileEntity toSave = filesFactory.newSecureFile(bucket, fileDefinition);
        final SecureFileEntity savedFile = secureFileRepository.save(toSave);
        final FileMetadataResource resource = fileMetadataResourceAssembler.toResource(savedFile);
        return new ResponseEntity<>(resource, locationHeader(resource), HttpStatus.CREATED);
    }

    private ResponseEntity<ResourceSupport> getRedirectResponseOrNotFound(URI location) {
        if (location == null) {
            log.info("Location was never found, returning 404");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } else {
            log.info("Location was found, setting headers");
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(location);
            final ResourceSupport resource = new ResourceSupport();
            resource.add(new Link(headers.getLocation().toASCIIString(), SELF.rel()));
            return new ResponseEntity<>(resource, headers, HttpStatus.CREATED);
        }
    }
}
