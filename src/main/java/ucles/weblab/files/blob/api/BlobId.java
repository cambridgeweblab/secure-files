package ucles.weblab.files.blob.api;

import java.io.Serializable;

/**
 * A Blob Id would be a file name for example.
 *
 * @author Sukhraj
 */
public final class BlobId implements Serializable {
    private final String id;

    public BlobId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final BlobId other = (BlobId) obj;
        return (this.id == null) ? other.id == null : this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.id == null ? 0 : this.id.hashCode());
        return hash;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " id[" + id + "]";
    }

}
