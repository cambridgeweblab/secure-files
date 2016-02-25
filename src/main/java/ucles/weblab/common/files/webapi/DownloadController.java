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
        
        //ask the cache if it's there
        Optional<PendingDownload> cacheEntry = recentDownloadCache.get(downloadId, collectionName, secureFile);
        String url;
        
        if (cacheEntry.isPresent()) {
            url = cacheEntry.get().getUrl();
        } else {
            //add it to the cache instead 
            PendingDownload pd = new PendingDownload(MediaType.valueOf(secureFile.getContentType()), secureFile.getFilename(), secureFile.getPlainData(), purgeTime, null);
            Optional<BlobStoreResult> putResult = recentDownloadCache.put(downloadId, collectionName, pd);
            Optional<String> urlOpt = recentDownloadCache.getUrl(downloadId, collectionName, pd);
            
            url = urlOpt.orElse("");
        }
              
        //get the link from above. 
        return URI.create(url);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<byte[]> fetchPreviouslyGeneratedDownload(@PathVariable String id) {
        final UUID downloadId = UUID.fromString(id);
        Optional<PendingDownload> pendingDownloadOptional = recentDownloadCache.get(downloadId, id, null);
        
        PendingDownload pendingDownload = pendingDownloadOptional.orElse(null);
                
        if (pendingDownload == null || pendingDownload.getPurgeTime().isBefore(Instant.now(clock))) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("private, max-age=300"); // The important thing is to avoid no-cache and no-store, for IE.
        headers.setContentType(pendingDownload.getContentType());
        headers.setContentLength(pendingDownload.getContent().length);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + pendingDownload.getFilename() + '"');
        headers.setLocation(linkTo(methodOn(DownloadController.class).fetchPreviouslyGeneratedDownload(downloadId.toString())).toUri());
        return new ResponseEntity<>(pendingDownload.getContent(), headers, HttpStatus.OK);
    }
}
