package ucles.weblab.common.files.domain.jpa;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import ucles.weblab.common.files.domain.SecureFileCollectionEntity;
import ucles.weblab.common.files.domain.SecureFileEntity;
import ucles.weblab.common.files.domain.SecureFileRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of a secure file repository.
 *
 * @since 18/03/15
 */
public interface SecureFileRepositoryJpa extends SecureFileRepository, Repository<SecureFileEntityJpa, UUID> {
    @Override
    Optional<SecureFileEntityJpa> findOneByCollectionAndFilename(SecureFileCollectionEntity collection, String filename);

    @Override
    SecureFileEntityJpa save(SecureFileEntity secureFile);

    @Override
    Collection<SecureFileEntityJpa> findAllByCollection(SecureFileCollectionEntity collection);

    @Override
    void delete(SecureFileEntity file);

    @Override
    @Modifying
    @Query("delete from SecureFile f where f in (select sf from SecureFile sf WHERE sf.collection.purgeInstant < ?)")
    Integer deleteByCollectionPurgeInstantBefore(Instant cutOff);
}
