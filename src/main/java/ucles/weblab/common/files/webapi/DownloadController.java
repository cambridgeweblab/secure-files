package ucles.weblab.common.files.webapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    private Duration cacheExpiry;
    private final ConcurrentHashMap<UUID, PendingDownload> recentDownloadCache = new ConcurrentHashMap<>();

    static class PendingDownload {
        final MediaType contentType;
        final String filename;
        final byte[] content;
        final Instant purgeTime;

        PendingDownload(MediaType contentType, String filename, byte[] content, Instant purgeTime) {
            this.contentType = contentType;
            this.content = content;
            this.filename = filename;
            this.purgeTime = purgeTime;
        }

        MediaType getContentType() {
            return contentType;
        }

        String getFilename() {
            return filename;
        }

        byte[] getContent() {
            return content;
        }

        Instant getPurgeTime() {
            return purgeTime;
        }
    }

    @Autowired(required = false) // will fall back to default system UTC clock
    public void configureClock(Clock clock) {
        log.warn("Clock overridden with " + clock);
        this.clock = clock;
    }

    @Autowired
    void configureCacheExpiry(@Value("${files.download.cache.expirySeconds:30}") int cacheExpirySeconds) {
        log.info("Cache expiry set to " + cacheExpirySeconds + "s");
        this.cacheExpiry = Duration.ofSeconds(cacheExpirySeconds);
    }

    /**
     * Scheduled job to clean up the cache every 15 minutes.
     */
    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void cleanCache() {
        final Iterator<Map.Entry<UUID, PendingDownload>> cacheEntries = recentDownloadCache.entrySet().iterator();
        while (cacheEntries.hasNext()) {
            Map.Entry<UUID, PendingDownload> cacheEntry =  cacheEntries.next();
            if (cacheEntry.getValue().getPurgeTime().isBefore(Instant.now(clock))) {
                cacheEntries.remove();
            }
        }
    }

    /**
     * This method is used by other controllers to generate downloads ready to be fetched by the browser.
     *
     * @param filename the filename for the download
     * @param contentType the content type of the download
     * @param content the download binary content
     * @return a time-limited URI for unauthenticated access to the download
     */
    public URI generateDownload(String filename, MediaType contentType, byte[] content) {
        UUID downloadId = UUID.randomUUID();
        recentDownloadCache.put(downloadId, new PendingDownload(contentType, filename, content, Instant.now(clock).plus(cacheExpiry)));

        return linkTo(methodOn(DownloadController.class).fetchPreviouslyGeneratedDownload(downloadId.toString())).toUri();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<byte[]> fetchPreviouslyGeneratedDownload(@PathVariable String id) {
        final UUID downloadId = UUID.fromString(id);
        PendingDownload pendingDownload = recentDownloadCache.get(downloadId);
        if (pendingDownload == null || pendingDownload.getPurgeTime().isBefore(Instant.now(clock))) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl("private, max-age=300"); // The important thing is to avoid no-cache and no-store, for IE.
        headers.setContentType(pendingDownload.getContentType());
        headers.setContentLength(pendingDownload.getContent().length);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + pendingDownload.getFilename());
        headers.setLocation(linkTo(methodOn(DownloadController.class).fetchPreviouslyGeneratedDownload(downloadId.toString())).toUri());
        return new ResponseEntity<>(pendingDownload.getContent(), headers, HttpStatus.OK);
    }
}
