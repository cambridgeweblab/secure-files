package ucles.weblab.common.blob.api;

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;

/**
 * A blob store service interface to describe the main actions that a blob 
 * store implementation must fulfill;
 * 
 * @author Sukhraj
 */
public interface BlobStoreService {
    
    /**
     * Clean up for shutdown purposes
     */
    public void dispose();

    /**
     * Save the blob 
     * @param id - with the specified name
     * @param mimeType
     * @param data
     * @param purgeTime
     * @return an Optional BlobStoreResult that will hold all information from the result of a 'put'
     * @throws BlobStoreException 
     */
    public Optional<BlobStoreResult> putBlob(BlobId id, String mimeType, byte data[], Instant purgeTime) throws BlobStoreException;
    
    public void putBlob(BlobId id, String mimeType, InputStream in, int length) throws BlobStoreException;

    public Optional<Blob> getBlob(BlobId id, boolean includeContent) throws BlobStoreException, BlobNotFoundException;
    
    public Optional<Blob> getBlobWithPartBlobId(String prefix, String suffix, boolean includeContent) throws BlobStoreException, BlobNotFoundException;
    
    public Optional<Long> getBlobSize(BlobId id) throws BlobStoreException, BlobNotFoundException;
    
    public void removeBlob(BlobId id) throws BlobStoreException;
    
    public void renameBlob(BlobId oldBlob, BlobId newBlob) throws BlobStoreException, BlobNotFoundException ;

    public Optional<URI> getUrl(BlobId blobId) throws BlobStoreException, BlobNotFoundException;
    
    public boolean exists(BlobId blobId);
}
