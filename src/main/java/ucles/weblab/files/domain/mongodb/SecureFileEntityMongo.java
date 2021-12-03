package ucles.weblab.files.domain.mongodb;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import ucles.weblab.files.domain.EncryptionService;
import ucles.weblab.files.domain.SecureFileEntity;

import java.io.DataInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Objects;

import static ucles.weblab.common.domain.ConfigurableEntitySupport.configureBean;

/**
 * Representation of a secure file, holding encrypted data in the {@link SecureFileRepositoryMongo}.
 * This class is only used for already-persisted files.
 *
 * @since 18/03/15
 */
@Configurable
public class SecureFileEntityMongo implements SecureFileEntity {
    public static final String CIPHER_PROPERTY = "cipher";
    public static final String FILE_KEY = "fileKey";
    public static final String NOTES_PROPERTY = "notes";
    public static final String FILENAME_PROPERTY = "filename";
    public static final String CONTENT_TYPE_PROPERTY = "contentType";

    private SecureFileCollectionEntityMongo bucket;
    private GridFsResource resource;
    private String filename;
    private String contentType;
    private String cipher;
    private String notes;
    private long length;
    /**
     * Unique key for the file.
     * To be combined with a system-wide secret key to decrypt or encrypt the file.
     */
    private byte[] fileKey;
    private byte[] encryptedData;

    private Instant createdDate;

    private EncryptionService encryptionService;

    @SuppressWarnings("UnusedDeclaration") // for testing
    SecureFileEntityMongo() {
        configureBean(this);
        this.encryptionService = null;
    }

    /**
     * Creates a new entity object from an existing GridFSDBFile record.
     * @param resource the MongoDB GridFS record
     */
    public SecureFileEntityMongo(SecureFileCollectionEntityMongo bucket, GridFsResource resource) throws IOException {
        this();
        this.bucket = bucket;
        this.resource = resource;
        this.filename = resource.getFilename();
        this.contentType = resource.getContentType();
        this.length = resource.contentLength();
        this.fileKey = ((ObjectId) resource.getId()).toByteArray();
        this.cipher = resource.getGridFSFile().getMetadata().getString(CIPHER_PROPERTY);
        this.notes = resource.getGridFSFile().getMetadata().getString(NOTES_PROPERTY);
        this.encryptedData = new byte[(int) this.length];
        try (DataInputStream dis = new DataInputStream(resource.getInputStream())) {
            dis.readFully(this.encryptedData);
        } catch (IOException ex) {
            throw new TransientDataAccessResourceException("Could not obtain file data", ex);
        }
    }

    public Object readResolve() {
        configureBean(this);
        return this;
    }

    @Autowired
    void configureEncryptionService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public boolean isNew() {
        return false;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Length of the file. Will be zero for newly created files.
     * @return the file.
     */
    public long getLength() {
        return length;
    }

    @Override
    public SecureFileCollectionEntityMongo getCollection() {
        return bucket;
    }

    GridFSFile getDbFile() {
        return resource.getGridFSFile();
    }

    @Override
    public String getNotes() {
        return notes;
    }

    @Override
    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public byte[] getEncryptedData() {
        return encryptedData;
    }

    @Override
    public byte[] getPlainData() {
        return encryptionService.decrypt(cipher, fileKey, encryptedData);
    }

    @Override
    public Instant getCreatedDate() {
        return this.createdDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SecureFileEntityMongo that = (SecureFileEntityMongo) o;
        return Objects.equals(length, that.length) &&
                Objects.equals(filename, that.filename) &&
                Objects.equals(contentType, that.contentType) &&
                Objects.equals(notes, that.notes) &&
                Objects.equals(fileKey, that.fileKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, contentType, notes, length, fileKey);
    }

    @Override
    public String toString() {
        return "SecureFileEntityMongo{" +
                "filename='" + filename + '\'' +
                ", contentType='" + contentType + '\'' +
                ", length=" + length +
                '}';
    }
}
