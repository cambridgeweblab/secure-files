package ucles.weblab.files.domain.mongodb;

import java.time.Instant;

/**
 * Implementation of those secure file repository which cannot be implemented automatically by spring-data-mongodb.
 *
 * @since 24/07/15
 */
public class SecureFileCollectionRepositoryMongoImpl implements SecureFileCollectionRepositoryMongoCustom {
    @Override
    public Long deleteByPurgeInstantBefore(Instant cutOff) {
        throw new UnsupportedOperationException("Purging not yet implemented for MongoDB.");
    }
}
