package ucles.weblab.files.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

/**
 * DDD repository interface - persistence-technology-neutral interface providing repository (i.e. CRUD) methods for
 * manipulating files.
 * <p>
 * Although this is technology neutral, it uses Spring Data naming conventions for methods. This allows the
 * interface to be extended with a Spring Data Repository interface for which an implementation is proxied in
 * at runtime.
 * </p>
 */
public interface SecureFileRepository {
    Optional<? extends SecureFileEntity> findOneByCollectionAndFilename(SecureFileCollectionEntity collection, String filename);

    /**
     * @param secureFile the entity to save
     * @return the saved entity. This may not be the same object passed in.
     */
    SecureFileEntity save(SecureFileEntity secureFile);

    Collection<? extends SecureFileEntity> findAllByCollection(SecureFileCollectionEntity collection);

    void delete(SecureFileEntity file);

    Integer deleteByCollectionPurgeInstantBefore(Instant cutOff);
}
