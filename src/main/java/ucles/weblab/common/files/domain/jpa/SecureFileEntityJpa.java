package ucles.weblab.common.files.domain.jpa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Persistable;
import ucles.weblab.common.files.domain.EncryptionService;
import ucles.weblab.common.files.domain.SecureFile;
import ucles.weblab.common.files.domain.SecureFileCollectionEntity;
import ucles.weblab.common.files.domain.SecureFileEntity;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.PostPersist;
import javax.persistence.Table;
import javax.persistence.Transient;

import static ucles.weblab.common.domain.ConfigurableEntitySupport.configureBean;

/**
 * Representation of a secure file, holding encrypted data in the {@link SecureFileRepositoryJpa}.
 *
 * @since 18/03/15
 */

@Entity(name = "SecureFile")
@Table(name = "secure_files")
@Configurable
public class SecureFileEntityJpa implements Persistable<UUID>, SecureFileEntity {
    public static final String COLLECTION = "collection";

    {
        configureBean(this);
    }

    public Object readResolve() {
        configureBean(this);
        return this;
    }

    @Id
    @Column(updatable = false)
    private UUID id;
    @Transient
    private boolean isNew;

    @ManyToOne
    private SecureFileCollectionEntityJpa collection;

    private String filename;

    private String contentType;

    @Column(updatable = false)
    private String cipher;

    @Column(updatable = false)
    private long length;

    private String notes;

    @Lob
    @Column(updatable = false)
    private byte[] encryptedData;

    @Transient
    private EncryptionService encryptionService;
    @Transient
    private String defaultCipherName;

    protected SecureFileEntityJpa() {
         // For Hibernate and Jackson
    }

    public SecureFileEntityJpa(SecureFileCollectionEntityJpa collection, SecureFile vo) {
        this.id = UUID.randomUUID();
        this.isNew = true;
        this.collection = collection;
        this.filename = vo.getFilename();
        this.contentType = vo.getContentType();
        this.length = vo.getLength();
        this.notes = vo.getNotes();
        this.cipher = defaultCipherName;
        this.encryptedData = encryptionService.encrypt(this.cipher, getFileKey(), vo.getPlainData());
    }

    @Autowired
    public void configureEncryptionService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Autowired
    public void configureDefaultCipher(@Value("${files.security.cipher:AES-GCM}") String cipherName) {
        this.defaultCipherName = cipherName;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getNotes() {
        return notes;
    }

    @Override
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Length of the file. Will be zero for newly created files.
     * @return the file.
     */
    @Override
    public long getLength() {
        return length;
    }

    @Override
    public SecureFileCollectionEntity getCollection() {
        return collection;
    }

    byte[] getEncryptedData() {
        return encryptedData;
    }

    @Override
    public byte[] getPlainData() {
        return encryptionService.decrypt(cipher, getFileKey(), encryptedData);
    }

    /**
     * Unique key for the file.
     * To be combined with a system-wide secret key to decrypt or encrypt the file.
     */
    byte[] getFileKey() {
        return ByteBuffer.allocate(16)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .array();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SecureFileEntityJpa that = (SecureFileEntityJpa) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SecureFileEntityJpa{" +
                "filename='" + filename + '\'' +
                ", contentType='" + contentType + '\'' +
                ", length=" + length +
                '}';
    }
}
