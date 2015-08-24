package ucles.weblab.common.files.domain.jpa;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.springframework.data.domain.Persistable;
import ucles.weblab.common.files.domain.SecureFileCollection;
import ucles.weblab.common.files.domain.SecureFileCollectionEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PostPersist;
import javax.persistence.Table;
import javax.persistence.Transient;

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
    private boolean isNew;

    @Column(unique = true)
    private String displayName;

    private Instant purgeInstant;

    @OneToMany(mappedBy = SecureFileEntityJpa.COLLECTION, cascade = {CascadeType.REMOVE})
    @Fetch(FetchMode.SUBSELECT)
    private List<SecureFileEntityJpa> files = new ArrayList<>();

    protected SecureFileCollectionEntityJpa() { // For Hibernate and Jackson
    }

    public SecureFileCollectionEntityJpa(SecureFileCollection vo) {
        this.displayName = vo.getDisplayName();
        this.purgeInstant = vo.getPurgeInstant().orElse(null);
        this.bucket = deriveBucket(displayName);
        this.isNew = true;
    }

    @Override
    public String getId() {
        return bucket;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    void markNotNew() {
        this.isNew = false;
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
