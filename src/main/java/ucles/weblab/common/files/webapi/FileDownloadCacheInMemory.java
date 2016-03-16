package ucles.weblab.common.files.webapi;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import ucles.weblab.common.files.blob.api.BlobId;
import ucles.weblab.common.files.blob.api.BlobStoreResult;
import ucles.weblab.common.files.domain.SecureFileMetadata;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;


/**
 *
 * @author sukhraj (taken from code from DownloadController with was originally 
 * written by gboden.
 */
public class FileDownloadCacheInMemory implements FileDownloadCache<UUID, PendingDownload> {
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ConcurrentHashMap<String, PendingDownload> recentDownloadCache;        
    private Clock clock = Clock.systemUTC();
    private Duration cacheExpiry;
    
    public FileDownloadCacheInMemory() {
        recentDownloadCache = new ConcurrentHashMap<>();
    }
    
    /**
     * Scheduled job to clean up the cache every 15 minutes.
     */
    @Override
    @Scheduled(fixedRate = 15 * 60 * 1000)
    public void clean() {
        final Iterator<Map.Entry<String, PendingDownload>> cacheEntries = recentDownloadCache.entrySet().iterator();
        while (cacheEntries.hasNext()) {
            Map.Entry<String, PendingDownload> cacheEntry =  cacheEntries.next();
            if (cacheEntry.getValue().getPurgeTime().isBefore(Instant.now(clock))) {
                cacheEntries.remove();
            }
        }
    }
       
    @Override
    public Optional<PendingDownload> get(UUID id, String collectionName, String fileName) {        
        final Iterator<Map.Entry<String, PendingDownload>> cacheEntries = recentDownloadCache.entrySet().iterator();
        while (cacheEntries.hasNext()) {
            Map.Entry<String, PendingDownload> cacheEntry =  cacheEntries.next();
            String cacheKey = cacheEntry.getKey();
            if (cacheKey.startsWith(collectionName) && cacheKey.endsWith(fileName.replaceAll("\\s+", "_"))) {
                return Optional.of(cacheEntry.getValue());
            }
        }
        
        return Optional.empty();
                    
    }
    
    
    @Override
    public Optional<BlobStoreResult> put(UUID id, String collectionName, PendingDownload pendingDownload) {
        
        String key = createCacheKey(id, collectionName, pendingDownload.getFilename());
        PendingDownload result = recentDownloadCache.put(key, pendingDownload );
        BlobStoreResult blobStoreResult = new BlobStoreResult(new BlobId(id.toString()), 
                                                              pendingDownload.getFilename(), 
                                                              collectionName, 
                                                              pendingDownload.getPurgeTime(), 
                                                              pendingDownload.getUrl()==null ? null : pendingDownload.getUrl().toString());
        return Optional.of(blobStoreResult);
    }
    
    @Override
    public Duration getExpiry() {
        return cacheExpiry;
    }
    
    @Override
    public boolean exists(UUID id, String collectionName, SecureFileMetadata secureFileMetadata) {
        String key = createCacheKey(id, collectionName, collectionName);
        PendingDownload pd = recentDownloadCache.get(key);
        return pd != null;
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

    @Override
    public Optional<URI> getUrl(UUID id, String collectionName, String fileName) {
        try {
            String url = ControllerLinkBuilder.linkTo(methodOn(DownloadController.class).fetchPreviouslyGeneratedDownload(collectionName, id.toString(),fileName )).toUri().toString();
            return Optional.of(URI.create(url));
        } catch (Exception e) {
            log.warn("Exception caught while getting url from ControllerLinkBuilder, returning empty optional", e);
            return Optional.empty();
        }
        
    }

    /*@Override
    public Optional<String> getRecentUrl(String collectionName, String fileName) {
        
        //always return empty
        return Optional.empty();
        
    } */

    @Override
    public URI getRedirectUrl(UUID id, String collectionName, String fileName) {
        return linkTo(methodOn(DownloadController.class).fetchPreviouslyGeneratedDownload(collectionName, id == null ? null : id.toString(), fileName)).toUri();
    }

    

}
