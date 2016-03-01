package ucles.weblab.common.blob.api;

/**
 *
*/
public class BlobStoreException extends Exception {
    
    public BlobStoreException(String message) {
        super(message);
    }

    public BlobStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
