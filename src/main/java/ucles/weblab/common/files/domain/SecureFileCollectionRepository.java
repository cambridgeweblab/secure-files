package ucles.weblab.common.files.domain;

import java.time.Instant;
import java.util.Collection;

/**
 * DDD repository interface - persistence-technology-neutral interface providing repository (i.e. CRUD) methods for
 * manipulating file collections.
 * <p>
 * Although this is technology neutral, it uses Spring Data naming conventions for methods. This allows the
 * interface to be extended with a Spring Data Repository interface for which an implementation is proxied in
 * at runtime.
 * </p>
 *
 * @since 05/06/15
 */
public interface SecureFileCollectionRepository {
    SecureFileCollectionEntity findOne(String id); // Required for ResourceReaderRepositoryPopulator

    SecureFileCollectionEntity save(SecureFileCollectionEntity s);

    Collection<? extends SecureFileCollectionEntity> findAll();

    SecureFileCollectionEntity findOneByDisplayName(String displayName);

    SecureFileCollectionEntity findOneByBucket(String bucket);

    Long removeByPurgeInstantBefore(Instant cutOff);
}
