package ucles.weblab.common.files.webapi;

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
     * @param id
     * @param collectionName
     * @param secureFile
     * @return a time-limited URI for unauthenticated access to the download
     */
    public URI generateDownload(UUID id, String collectionName, SecureFileEntity secureFile) {
        return recentDownloadCache.getRedirectUrl(id, collectionName, secureFile.getFilename());
    }
    
    /**
     * This method is responsible for making the browser display a file>save as
     * dialog box. If there is no trailing slash, then it does not cause a save-as
     * box to appear. 
     * 
     * @param id
     * @param fileName
     * @return 
     */
    @RequestMapping(value = "/{id}/{fileName}/", 
                    method = RequestMethod.GET)
    public ResponseEntity<byte[]> fetchPreviouslyGeneratedDownload(@PathVariable String id, @PathVariable String fileName) {
        final UUID downloadId = UUID.fromString(id);
        
        Optional<PendingDownload> pendingDownloadOptional = recentDownloadCache.get(downloadId, id, fileName);
        
        PendingDownload pendingDownload = pendingDownloadOptional.orElse(null);
                
        if (pendingDownload == null || pendingDownload.getPurgeTime().isBefore(Instant.now(clock))) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        
        URI toUri = linkTo(methodOn(DownloadController.class).fetchPreviouslyGeneratedDownload(downloadId.toString(), fileName)).toUri();
        HttpHeaders headers = getDownloadHeaders(pendingDownload.getFilename(), pendingDownload.getContentType(), pendingDownload.getContent().length, toUri);
        return new ResponseEntity<>(pendingDownload.getContent(), headers, HttpStatus.OK);
    }
    
    /**
     * This method is responsible for displaying a file (in the browser) of an external file
     * @param collectionName
     * @param fileName
     * @param id
     * @return 
     */
    @RequestMapping(value = "/redirectfile/{collectionName}/{fileName}/{id}", 
                    method = RequestMethod.GET)
    public ResponseEntity<Object> redirectToExternalUrl(@PathVariable String collectionName, 
                                                        @PathVariable String fileName, 
                                                        @PathVariable UUID id) {
        
        Optional<PendingDownload> pendingDownloadOptional = recentDownloadCache.get(id, collectionName, fileName);
        
        if (!pendingDownloadOptional.isPresent() || pendingDownloadOptional.get().getPurgeTime().isBefore(Instant.now(clock))) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        HttpHeaders headers = new HttpHeaders();
        // The important thing is to avoid no-cache and no-store, for IE.
        headers.setCacheControl("private, max-age=300"); 
        headers.setContentType(pendingDownloadOptional.get().getContentType());
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + '"');
        Optional<String> url = recentDownloadCache.getUrl(id, collectionName, fileName);
        URI location = null;
        if (url.isPresent()) {
            location = URI.create(url.get());
        }
        log.info("Setting location to save from as: " + location);
        headers.setLocation(location);
                
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
        
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
