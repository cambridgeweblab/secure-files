package ucles.weblab.common.files.blob.api;

import java.time.Instant;

/**
 * An object to hold all the returned data from a blobstore interaction. 
 * 
 * @author Sukhraj
 */
public class BlobStoreResult {
    
    private final BlobId blobId; 
    private final String displayName;
    private final Instant purgeInstant; 
    private final String rootPath; 
    private final String urlToFile;
    
    public BlobStoreResult(BlobId blobId, 
                           String displayName, 
                           String rootPath, 
                           Instant purgeInstant, 
                           String urlToFile) {
        
        this.blobId = blobId;
        this.displayName = displayName;
        this.purgeInstant = purgeInstant;
        this.rootPath = rootPath;
        this.urlToFile = urlToFile;
    }

    public BlobId getBlobId() {
        return blobId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getPurgeInstant() {
        return purgeInstant;
    }

    public String getRootPath() {
        return rootPath;
    }

    public String getUrlToFile() {
        return urlToFile;
    }
    
}
