package ucles.weblab.common.files.domain.mongodb;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import ucles.weblab.common.domain.mongodb.AbstractEntity;
import ucles.weblab.common.files.domain.SecureFileCollection;
import ucles.weblab.common.files.domain.SecureFileCollectionEntity;

import java.time.Instant;
import java.util.Optional;

/**
 * Collection of files. These are held in the same bucket in MongoDB, and also have a purge date associated with the
 * collection after which the whole bucket will be deleted.
 *
 * @since 18/03/15
 */

@SuppressWarnings("CanBeFinal")
@Document(collection = "collections", language = "english")
public class SecureFileCollectionEntityMongo extends AbstractEntity implements SecureFileCollectionEntity {

    @Indexed(unique = true)
    private String displayName;
    @Indexed(unique = true)
    private String bucket;
    private Instant purgeInstant;

    @SuppressWarnings("UnusedDeclaration")
    protected SecureFileCollectionEntityMongo() { // For Jackson
    }

    public SecureFileCollectionEntityMongo(SecureFileCollection vo) {
        this.displayName = vo.getDisplayName();
        this.purgeInstant = vo.getPurgeInstant().orElse(null);
        this.bucket = deriveBucket(displayName);
    }

    public String getDisplayName() {
        return displayName;
    }

    public Optional<Instant> getPurgeInstant() {
        return Optional.ofNullable(purgeInstant);
    }

    public String getBucket() {
        return bucket;
    }

    @Override
    public String toString() {
        return "SecureFileCollectionMongo{" +
                "displayName='" + displayName + '\'' +
                ", bucket='" + bucket + '\'' +
                ", purgeInstant=" + purgeInstant +
                '}';
    }
}
