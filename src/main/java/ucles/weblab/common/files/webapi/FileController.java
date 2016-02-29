package ucles.weblab.common.files.webapi;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ucles.weblab.common.webapi.AccessAudited;
import ucles.weblab.common.webapi.exception.ResourceNotFoundException;
import ucles.weblab.common.files.domain.FilesFactory;
import ucles.weblab.common.files.domain.SecureFile;
import ucles.weblab.common.files.domain.SecureFileCollection;
import ucles.weblab.common.files.domain.SecureFileCollectionEntity;
import ucles.weblab.common.files.domain.SecureFileCollectionRepository;
import ucles.weblab.common.files.domain.SecureFileEntity;
import ucles.weblab.common.files.domain.SecureFileRepository;
import ucles.weblab.common.files.webapi.converter.FileCollectionResourceAssembler;
import ucles.weblab.common.files.webapi.converter.FileMetadataResourceAssembler;
import ucles.weblab.common.files.webapi.resource.FileCollectionResource;
import ucles.weblab.common.files.webapi.resource.FileMetadataResource;

import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucles.weblab.common.blob.api.BlobStoreResult;

import static java.util.stream.Collectors.toList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;
import static ucles.weblab.common.webapi.HateoasUtils.locationHeader;
import static ucles.weblab.common.webapi.LinkRelation.SELF;
import static ucles.weblab.common.webapi.MoreMediaTypes.APPLICATION_JSON_UTF8_VALUE;

/**
 * Web API for dealing with secure files.
 *
 * @since 19/03/15
 */
@RestController
@RequestMapping("/api/files")
public class FileController {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final FilesFactory filesFactory;
    private final SecureFileCollectionRepository secureFileCollectionRepository;
    private final SecureFileRepository secureFileRepository;
    private final FileMetadataResourceAssembler fileMetadataResourceAssembler;
    private final FileCollectionResourceAssembler fileCollectionResourceAssembler;
    private final DownloadController downloadController;
    private final Supplier<SecureFileCollection.Builder> secureFileCollectionBuilder;
    private final Supplier<SecureFile.Builder> secureFileBuilder;
    private final FileDownloadCache<UUID, PendingDownload> downloadCache;
    private Clock clock = Clock.systemUTC();
    
    private static class DecryptionFailedException extends NestedRuntimeException {
        public DecryptionFailedException(Exception e) {
            super("Failed to decrypt file data", e);
        }
    }

    @Autowired
    public FileController(FilesFactory filesFactory, 
                          SecureFileCollectionRepository secureFileCollectionRepository, 
                          SecureFileRepository secureFileRepository,
                          FileMetadataResourceAssembler fileMetadataResourceAssembler,
                          FileCollectionResourceAssembler fileCollectionResourceAssembler, 
                          DownloadController downloadController,
                          Supplier<SecureFileCollection.Builder> secureFileCollectionBuilder, 
                          Supplier<SecureFile.Builder> secureFileBuilder,
                          FileDownloadCache<UUID, PendingDownload> downloadCache) {
        this.filesFactory = filesFactory;
        this.secureFileCollectionRepository = secureFileCollectionRepository;
        this.secureFileRepository = secureFileRepository;
        this.fileMetadataResourceAssembler = fileMetadataResourceAssembler;
        this.fileCollectionResourceAssembler = fileCollectionResourceAssembler;
        this.downloadController = downloadController;
        this.secureFileCollectionBuilder = secureFileCollectionBuilder;
        this.secureFileBuilder = secureFileBuilder;
        this.downloadCache = downloadCache;
    }

    @RequestMapping(value = "/", method = RequestMethod.GET, produces = APPLICATION_JSON_UTF8_VALUE)
    @PreAuthorize("isAuthenticated()")
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
        return ResponseEntity.ok(secureFileRepository.findAllByCollection(collection).stream()
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

        final Optional<? extends SecureFileEntity> found = secureFileRepository.findOneByCollectionAndFilename(collection, filename);
        return found
                .map(secureFile -> ResponseEntity.ok(fileMetadataResourceAssembler.toResource(secureFile)))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @RequestMapping(value = "/{bucket}/{filename}/", method = RequestMethod.PUT, consumes = APPLICATION_JSON_UTF8_VALUE, produces = APPLICATION_JSON_UTF8_VALUE)
    @AccessAudited
    public FileMetadataResource updateFileMetadata(@PathVariable String bucket, @PathVariable String filename, @RequestBody FileMetadataResource update) {
        SecureFileCollectionEntity collection = secureFileCollectionRepository.findOneByBucket(bucket);
        if (collection == null) throw new ResourceNotFoundException(bucket);
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
        if (collection == null) throw new ResourceNotFoundException(bucket);
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
    @PreAuthorize("isAuthenticated()")
    @AccessAudited
    public ResponseEntity<ResourceSupport> redirectToDownload(@PathVariable String bucket, @PathVariable String filename) {
        final SecureFileCollectionEntity collection = secureFileCollectionRepository.findOneByBucket(bucket);
        if (collection == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        final Optional<? extends SecureFileEntity> found = secureFileRepository.findOneByCollectionAndFilename(collection, filename);
        return found.map((secureFile) -> {
            try {                        
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.valueOf(secureFile.getContentType()));
                headers.setLocation(downloadController.generateDownload(UUID.randomUUID(), bucket, secureFile));
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
     * @param bucket
     * @param filename
     * @return 
     */
    @RequestMapping(value = "/{bucket}/{filename}/download/", 
                    method = RequestMethod.POST, 
                    produces = APPLICATION_JSON_UTF8_VALUE)
    @PreAuthorize("isAuthenticated()")
    @AccessAudited
    public ResponseEntity<ResourceSupport> generateDownloadLink(@PathVariable String bucket, 
                                                                @PathVariable String filename) {        
        //get the collection name 
        final SecureFileCollectionEntity collection = secureFileCollectionRepository.findOneByBucket(bucket);
        if (collection == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        //always create a random id, first time round
        UUID id = UUID.randomUUID();
        
        //get it from the cache
        Optional<PendingDownload> fileOpt = downloadCache.get(id, bucket, filename);
        final URI location;     
        
        if (fileOpt.isPresent()) {
            //if it's there, then set the location 
            location = fileOpt.get().getUrl();
        } else {
            final Optional<? extends SecureFileEntity> found = secureFileRepository.findOneByCollectionAndFilename(collection, filename);
            if (found.isPresent()) {
                //get the file
                SecureFileEntity secureFile = found.get();
                                
                //get the url
                Optional<URI> res = downloadCache.getUrl(id, bucket, secureFile.getFilename());
                                
                //create PendingDownload to save
                PendingDownload pd = new PendingDownload(MediaType.valueOf(secureFile.getContentType()), 
                                                         secureFile.getFilename(), 
                                                         secureFile.getPlainData(), 
                                                         Instant.now(clock).plus(this.downloadCache.getExpiry()), 
                                                         res.isPresent() ? res.get() : null);
                //put it in the cache
                Optional<BlobStoreResult> putResult = downloadCache.put(id, bucket, pd);               
                
                //get location
                location = downloadController.generateDownload(id, bucket, found.get());  
                
            } else {
                //it's not in the cache nor the repository
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }            
        }
               
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);
        final ResourceSupport resource = new ResourceSupport();        
        resource.add(new Link(headers.getLocation().toASCIIString(), SELF.rel()));
        return new ResponseEntity<>(resource, headers, HttpStatus.CREATED);

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
}
