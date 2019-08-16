package ucles.weblab.files.blob.api;

import java.io.Serializable;
import java.time.Instant;

/**
 * A blob encapsulates a file and stores data as a byte array.
 *
 * @author Sukhraj
 */
public final class Blob implements Serializable {

    private final BlobId id;
    private final String mimeType;
    private final byte[] data;
    private final Instant expiryDate;
    private final String url;

    public Blob(BlobId id, String mimeType, byte[] data, Instant expiryDate, String url) {
        this.id = id;
        this.mimeType = mimeType;
        this.data = data.clone();
        this.expiryDate = expiryDate;
        this.url = url;
    }

    public BlobId getId() {
        return id;
    }

    public String getMimeType() {
        return mimeType;
    }

    public byte[] getData() {
        return data;
    }

    public Instant getExpiryDate() {
        return expiryDate;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " id[" + id + "] mimeType[" + mimeType + "]";
    }
}
