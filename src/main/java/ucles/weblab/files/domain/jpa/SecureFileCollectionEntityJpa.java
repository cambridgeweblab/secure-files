package ucles.weblab.files.domain.jpa;

import org.springframework.data.domain.Persistable;
import ucles.weblab.files.domain.SecureFileCollection;
import ucles.weblab.files.domain.SecureFileCollectionEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.time.Instant;
import java.util.Optional;

/**
 * Collection of files. These have a purge date associated with the
 * collection after which the whole bucket will be deleted.
 *
 * @since 18/03/15
 */

@SuppressWarnings("CanBeFinal")
@Entity(name = "SecureFileCollection")
@Table(name = "secure_file_collections")
public class SecureFileCollectionEntityJpa implements Persistable<String>, SecureFileCollectionEntity {

    @Id
    private String bucket;

    @Transient
    private boolean unsaved;

    @Column(unique = true)
    private String displayName;

    private Instant purgeInstant;

    protected SecureFileCollectionEntityJpa() { // For Hibernate and Jackson
    }

    public SecureFileCollectionEntityJpa(SecureFileCollection vo) {
        this.displayName = vo.getDisplayName();
        this.purgeInstant = vo.getPurgeInstant().orElse(null);
        this.bucket = deriveBucket(displayName);
        this.unsaved = true;
    }

    @Override
    public String getId() {
        return bucket;
    }

    @Override
    public boolean isNew() {
        return unsaved;
    }

    @PostPersist
    void markNotNew() {
        this.unsaved = false;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Optional<Instant> getPurgeInstant() {
        return Optional.ofNullable(purgeInstant);
    }

    @Override
    public String getBucket() {
        return bucket;
    }

    @Override
    public String toString() {
        return "SecureFileCollectionEntityJpa{" +
                "bucket='" + bucket + '\'' +
                ", displayName='" + displayName + '\'' +
                ", purgeInstant=" + purgeInstant +
                '}';
    }
}
