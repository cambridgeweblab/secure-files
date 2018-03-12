package ucles.weblab.files.domain.mongodb;

import java.time.Instant;

/**
 * Declaration of secure file collection repository methods which cannot be implemented automatically by
 * spring-data-mongodb.
 *
 * @since 24/07/15
 */
public interface SecureFileCollectionRepositoryMongoCustom {
    Long deleteByPurgeInstantBefore(Instant cutOff);
}
