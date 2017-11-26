package ucles.weblab.common.files.domain.jpa;

import java.time.Instant;
import org.hibernate.annotations.Immutable;
import ucles.weblab.common.files.domain.SecureFileMetadataEntity;

import java.util.Objects;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Representation of a secure file, without its data, in the {@link SecureFileMetadataRepositoryJpa}. Intended for
 * reading metadata without pulling down the file data, for efficiency.
 *
 * @since 16/03/2016
 */
@Entity(name = "SecureFileMetadata")
@Immutable
@Table(name = "secure_files")
public class SecureFileMetadataEntityJpa implements SecureFileMetadataEntity {
    @Id
    private UUID id;

    @ManyToOne
    private SecureFileCollectionEntityJpa collection;

    private String filename;

    private String contentType;

    private long length;

    private String notes;

    private Instant createdDate;
    
    protected SecureFileMetadataEntityJpa() {
         // For Hibernate and Jackson
    }

    public UUID getId() {
        return id;
    }

    @Override
    public SecureFileCollectionEntityJpa getCollection() {
        return collection;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public String getNotes() {
        return notes;
    }
    
    @Override
    public Instant getCreatedDate() {
        return createdDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SecureFileMetadataEntityJpa that = (SecureFileMetadataEntityJpa) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SecureFileMetadataEntityJpa{" +
                "filename='" + filename + '\'' +
                ", contentType='" + contentType + '\'' +
                ", length=" + length +
                '}';
    }
}
