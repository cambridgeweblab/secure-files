package ucles.weblab.common.files.blob.api;

import java.io.InputStream;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * A blob store service interface to describe the main actions that a blob 
 * store implementation must fulfill. Most of these will throw BlobStoreException or
 * BlobNotFoundException which the caller must handle. 
 * 
 * @author Sukhraj
 */
public interface BlobStoreService {
    
    /**
     * Clean up for shutdown purposes
     */
    void dispose();

    /**
     * Save the blob 
     * @param id - with the specified name
     * @param mimeType
     * @param data
     * @param purgeTime
     * @return an Optional BlobStoreResult that will hold all information from the result of a 'put'
     * @throws BlobStoreException 
     */
    Optional<BlobStoreResult> putBlob(BlobId id, String mimeType, byte data[], Instant purgeTime) throws BlobStoreException;
    
    /**
     * Save the blob in the blob implementation. 
     * @param id - the name of blob 
     * @param mimeType - the content type of the blob
     * @param in - save the object using the data in the input stream
     * @param length - the length of data to save. 
     * @throws BlobStoreException 
     */
    void putBlob(BlobId id, String mimeType, InputStream in, int length) throws BlobStoreException;

    /**
     * Get the blob with the specified id. 
     * @param id - the id of blob to retrieve. 
     * @param includeContent - Optionally choose to read the byte data from the blob. This will have memory implications.
     * @return
     * @throws BlobStoreException
     * @throws BlobNotFoundException 
     */
    Optional<Blob> getBlob(BlobId id, boolean includeContent) throws BlobStoreException, BlobNotFoundException;
    
    /**
     * Get a blob with an id that matches the prefix passed in and has the same suffix as parameter. Both
     * conditions must be true to be returned. 
     * @param prefix - the prefix of the blob id to match. 
     * @param suffix - the suffix of the blob if to match.
     * @param includeContent - optionally include the byte data of the file. This will have memory implications. 
     * @return
     * @throws BlobStoreException
     * @throws BlobNotFoundException 
     */
    Optional<Blob> getBlobWithPartBlobId(String prefix, String suffix, boolean includeContent) throws BlobStoreException, BlobNotFoundException;
    
    /**
     * Get the blob size of the specified blob with the specified id. 
     * @param id - the id to search for 
     * @return
     * @throws BlobStoreException
     * @throws BlobNotFoundException 
     */
    Optional<Long> getBlobSize(BlobId id) throws BlobStoreException, BlobNotFoundException;
    
    /**
     * Remove the blob with the Id matching the id passed in 
     * @param id - the name to match when deleting 
     * @throws BlobStoreException 
     */
    void removeBlob(BlobId id) throws BlobStoreException;
    
    /**
     * Rename the file represented by oldBlob with the id of new blob Id. 
     * @param oldBlob - the old blob 
     * @param newBlob
     * @throws BlobStoreException
     * @throws BlobNotFoundException 
     */
    void renameBlob(BlobId oldBlob, BlobId newBlob) throws BlobStoreException, BlobNotFoundException ;

    /**
     * Each implementation must provide a way to get the url of the Blob for the specified blob id. 
     * @param blobId
     * @return
     * @throws BlobStoreException
     * @throws BlobNotFoundException 
     */
    Optional<URI> getUrl(BlobId blobId) throws BlobStoreException, BlobNotFoundException;
    
    /**
     * Return a flag if the blob id exists in the blob store implementation. 
     * @param blobId
     * @return 
     */
    boolean exists(BlobId blobId);
    
    /**
     * List all blob id for all blobs in blob store implementation. 
     * 
     * @param includeContent - optionally include content, will have memory implications. 
     * @return
     * @throws BlobStoreException 
     * @throws BlobNotFoundException 
     */
    List<Blob> listBlobs(boolean includeContent) throws BlobStoreException, BlobNotFoundException;
    
    /**
     * Expose the root folder
     * @return 
     */
    String getBucketName();
}
