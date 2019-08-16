package ucles.weblab.files.blob.api;

/**
 *
 */
public class BlobNotFoundException extends Exception {

    public BlobNotFoundException(String message) {
        super(message);
    }

    public BlobNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
