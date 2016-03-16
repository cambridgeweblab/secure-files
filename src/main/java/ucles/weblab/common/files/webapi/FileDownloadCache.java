package ucles.weblab.common.files.webapi;

import java.io.Serializable;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import ucles.weblab.common.files.blob.api.BlobStoreResult;
import ucles.weblab.common.files.domain.SecureFileMetadata;

/**
 *
 * @author Sukhraj
 * @param <T>
 * @param <PendingDownload>
 */
public interface FileDownloadCache<T extends Serializable, PendingDownload> {
    
    void clean();
    
    Optional<PendingDownload> get(T id, String collectionName, String fileName);
        
    Optional<BlobStoreResult> put(T id, String collectionName, PendingDownload pendingDownload);
    
    Duration getExpiry();
    
    boolean exists(T id, String collectionName, SecureFileMetadata secureFileMetadata);
    
    Optional<URI> getUrl(T id, String collectionName, String fileName);
    
    //Optional<String> getRecentUrl(String collectionName, String fileName);
    
    URI getRedirectUrl(UUID id, String collectionName, String fileName);
    
    /**
     * 
     * @param id
     * @param collectionName
     * @param fileName
     * @return 
     */
    default String createCacheKey(T id, String collectionName, String fileName) {
        String fn = collectionName + "_" + id + "_" + fileName;
        return fn.replaceAll("\\s+", "_");
    }

}
