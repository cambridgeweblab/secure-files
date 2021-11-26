package ucles.weblab.files.webapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import ucles.weblab.files.domain.SecureFileEntity;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;


import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * This controller serves out the actual downloads. They have a time-limited URI (30 seconds only) but must be
 * served out without authentication since the browser cannot supply the Authorization header required and AJAX
 * requests cannot result in downloads. They must also be served out with caching enabled otherwise Internet Explorer
 * will refuse to save the files.
 */
@RestController
@RequestMapping("/downloads")
public class DownloadController {
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
     * @param collectionName
     * @param id
     * @param fileName
     * @return
     */
    @RequestMapping(value = "/{collectionName}/{id}/{fileName}/",
                    method = RequestMethod.GET)
    public ResponseEntity<byte[]> fetchPreviouslyGeneratedDownload(@PathVariable String collectionName,
                                                                   @PathVariable String id,
                                                                   @PathVariable String fileName) {
        final UUID downloadId = UUID.fromString(id);

        Optional<PendingDownload> pendingDownloadOptional = recentDownloadCache.get(downloadId, collectionName, fileName);

        PendingDownload pendingDownload = pendingDownloadOptional.orElse(null);

        if (pendingDownload == null || pendingDownload.getPurgeTime().isBefore(Instant.now(clock))) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // TODO: validate that toUri() is OK and doesn't need replacing with URI.create(...toString()) to avoid double-encoding.
        URI toUri = linkTo(methodOn(DownloadController.class).fetchPreviouslyGeneratedDownload(downloadId.toString(), collectionName, fileName)).toUri();
        HttpHeaders headers = new HttpHeaders();
        // The important thing is to avoid no-cache and no-store, for IE.
        headers.setCacheControl("private, max-age=300");
        headers.setContentType(pendingDownload.getContentType());
        headers.setContentLength(pendingDownload.getContent().length);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + '"');
        headers.setLocation(toUri);
        return new ResponseEntity<>(pendingDownload.getContent(), headers, HttpStatus.OK);
    }

}
