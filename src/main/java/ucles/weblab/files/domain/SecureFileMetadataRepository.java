package ucles.weblab.files.domain;

import java.util.Collection;
import java.util.Optional;

/**
 * DDD repository interface - persistence-technology-neutral interface providing read-only repository methods for
 * obtaining file metadata. This will typically be implemented to point at the same physical store as the {@link SecureFileRepository}
 * but can have an optimised implementation which avoids loading the file data.
 * <p>
 * Although this is technology neutral, it uses Spring Data naming conventions for methods. This allows the
 * interface to be extended with a Spring Data Repository interface for which an implementation is proxied in
 * at runtime.
 * </p>
 */
public interface SecureFileMetadataRepository {
    Optional<? extends SecureFileMetadataEntity> findOneByCollectionAndFilename(SecureFileCollectionEntity collection, String filename);

    Collection<? extends SecureFileMetadataEntity> findAllByCollection(SecureFileCollectionEntity collection);
}
