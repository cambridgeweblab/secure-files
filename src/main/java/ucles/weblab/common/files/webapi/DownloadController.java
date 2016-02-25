package ucles.weblab.common.files.webapi;

import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.bind.annotation.RequestParam;
import ucles.weblab.common.blob.api.BlobStoreResult;
import ucles.weblab.common.files.domain.SecureFileEntity;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

/**
 * This controller serves out the actual downloads. They have a time-limited URI (30 seconds only) but must be
 * served out without authentication since the browser cannot supply the Authorization header required and AJAX
 * requests cannot result in downloads. They must also be served out with caching enabled otherwise Internet Explorer
 * will refuse to save the files.
 */
@RestController
@RequestMapping("/downloads")
public class DownloadController {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private Clock clock = Clock.systemUTC();
    private FileDownloadCache<UUID, PendingDownload> recentDownloadCache;
    
    @Autowired
    public DownloadController(FileDownloadCache recentDownloadCache) {
        this.recentDownloadCache = recentDownloadCache;
    }
    
    /**
     * This method is used by other controllers to generate downloads ready to be fetched by the browser.
     *
     * @param collectionName
     * @param secureFile
     * @return a time-limited URI for unauthenticated access to the download
     */
    public URI generateDownload(String collectionName, SecureFileEntity secureFile) {
        UUID downloadId = UUID.randomUUID();
        Instant purgeTime = Instant.now(clock).plus(this.recentDownloadCache.getExpiry());
        PendingDownload pd = new PendingDownload(MediaType.valueOf(secureFile.getContentType()), secureFile.getFilename(), secureFile.getPlainData(), purgeTime, null);
        Optional<BlobStoreResult> putResult = recentDownloadCache.put(downloadId, collectionName, pd);
        Optional<String> urlOpt = recentDownloadCache.getUrl(downloadId, collectionName, pd);

        String urlToFile = urlOpt.orElse(null);                  

        String fileName = recentDownloadCache.createCacheEntryKey(downloadId, collectionName, secureFile.getFilename());
        //return linkTo(methodOn(DownloadController.class).fetchPreviouslyGeneratedDownload(downloadId.toString())).toUri();
        URI toUri = linkTo(methodOn(DownloadController.class).redirectToExternalUrl(fileName)).toUri();
        log.info("Returning after generateDownload: " + toUri);
        return linkTo(methodOn(DownloadController.class).redirectToExternalUrl(fileName)).toUri();

       /// return returnLink;
       // return URI.create(urlToFile);

    }
    
    @RequestMapping(value = "/{id}", 
                    method = RequestMethod.GET)
    public ResponseEntity<byte[]> fetchPreviouslyGeneratedDownload(@PathVariable String id) {
        final UUID downloadId = UUID.fromString(id);
        Optional<PendingDownload> pendingDownloadOptional = recentDownloadCache.get(downloadId, id, null);
        
        PendingDownload pendingDownload = pendingDownloadOptional.orElse(null);
                
        if (pendingDownload == null || pendingDownload.getPurgeTime().isBefore(Instant.now(clock))) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        URI toUri = linkTo(methodOn(DownloadController.class).fetchPreviouslyGeneratedDownload(downloadId.toString())).toUri();
        HttpHeaders headers = getDownloadHeaders(pendingDownload.getFilename(), pendingDownload.getContentType(), pendingDownload.getContent().length, toUri);
        return new ResponseEntity<>(pendingDownload.getContent(), headers, HttpStatus.OK);
    }
    
    @RequestMapping(value = "/redirectfile/{fileName}/", 
                    method = RequestMethod.GET)
    public ResponseEntity<Object> redirectToExternalUrl(@PathVariable String fileName) {
        
        //no length set*******943502
        HttpHeaders httpHeaders = new HttpHeaders();
        // The important thing is to avoid no-cache and no-store, for IE.
        httpHeaders.setCacheControl("private, max-age=300"); 
        httpHeaders.setContentType(MediaType.valueOf("application/pdf"));

        httpHeaders.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + '"');
        httpHeaders.setLocation(URI.create("https://s3-eu-west-1.amazonaws.com/dfd-898089864934/dfd/files/" + fileName));
                
        return new ResponseEntity<>(httpHeaders, HttpStatus.OK);
        
    }
    
    private HttpHeaders getDownloadHeaders(String fileName, MediaType contentType, int length, URI uri) {
        HttpHeaders headers = new HttpHeaders();
        // The important thing is to avoid no-cache and no-store, for IE.
        headers.setCacheControl("private, max-age=300"); 
        headers.setContentType(contentType);
        headers.setContentLength(length);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + '"');
        headers.setLocation(uri);
        return headers;
    }
    
}
