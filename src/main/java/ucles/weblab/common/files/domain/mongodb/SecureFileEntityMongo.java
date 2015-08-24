package ucles.weblab.common.files.domain.mongodb;

import com.mongodb.gridfs.GridFSDBFile;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.dao.TransientDataAccessResourceException;
import ucles.weblab.common.files.domain.EncryptionService;
import ucles.weblab.common.files.domain.SecureFileEntity;

import java.io.DataInputStream;
import java.io.IOException;
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
    public static final String NOTES_PROPERTY = "notes";
    public static final String FILENAME_PROPERTY = "filename";
    public static final String CONTENT_TYPE_PROPERTY = "contentType";

    {
        configureBean(this);
    }

    public Object readResolve() {
        configureBean(this);
        return this;
    }

    private SecureFileCollectionEntityMongo bucket;
    private GridFSDBFile dbFile;
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

    private EncryptionService encryptionService;

    @SuppressWarnings("UnusedDeclaration") // for testing
    SecureFileEntityMongo() {
        this.encryptionService = null;
    }

    /**
     * Creates a new entity object from an existing GridFSDBFile record.
     * @param dbFile the MongoDB GridFS record
     * @throws IOException
     */
    public SecureFileEntityMongo(SecureFileCollectionEntityMongo bucket, GridFSDBFile dbFile) {
        this.bucket = bucket;
        this.dbFile = dbFile;
        this.filename = dbFile.getFilename();
        this.contentType = dbFile.getContentType();
        this.length = dbFile.getLength();
        this.fileKey = ((ObjectId) dbFile.getId()).toByteArray();
        this.cipher = (String) dbFile.get(CIPHER_PROPERTY);
        this.notes = (String) dbFile.get(NOTES_PROPERTY);
        this.encryptedData = new byte[(int) this.length];
        try (DataInputStream dis = new DataInputStream(dbFile.getInputStream())) {
            dis.readFully(this.encryptedData);
        } catch (IOException ex) {
            throw new TransientDataAccessResourceException("Could not obtain file data", ex);
        }
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

    GridFSDBFile getDbFile() {
        return dbFile;
    }

    @Override
    public String getNotes() {
        return notes;
    }

    @Override
    public void setNotes(String notes) {
        this.notes = notes;
    }

    byte[] getEncryptedData() {
        return encryptedData;
    }

    @Override
    public byte[] getPlainData() {
        return encryptionService.decrypt(cipher, fileKey, encryptedData);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
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
