package ucles.weblab.common.files.webapi;

import java.io.Serializable;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import ucles.weblab.common.blob.api.BlobStoreResult;
import ucles.weblab.common.files.domain.SecureFile;

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
    
    //why do we need this?????!!!! it does a get anyway on the cache. 
    boolean exists(T id, String collectionName, SecureFile secureFile);
    
    Optional<String> getUrl(T id, String collectionName, String fileName);
    
    //Optional<String> getRecentUrl(String collectionName, String fileName);
    
    /**
     * 
     * @param id
     * @param collectionName
     * @param fileName
     * @return 
     */
    default String createCacheEntryKey(T id, String collectionName, String fileName) {
        String fn = collectionName + "_" + id + "_" + fileName;
        return fn.replaceAll("\\s+", "_");
    }

}
