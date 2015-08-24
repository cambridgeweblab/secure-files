package ucles.weblab.common.files.domain.jpa;

import org.springframework.data.repository.Repository;
import ucles.weblab.common.files.domain.SecureFileCollectionEntity;
import ucles.weblab.common.files.domain.SecureFileCollectionRepository;

import java.time.Instant;
import java.util.Collection;

/**
 * Implementation of a secure file collection repository.
 */
public interface SecureFileCollectionRepositoryJpa extends Repository<SecureFileCollectionEntityJpa, String>, SecureFileCollectionRepository {
    @Override
    SecureFileCollectionEntityJpa findOne(String id);

    @Override
    SecureFileCollectionEntityJpa save(SecureFileCollectionEntity s);

    @Override
    Collection<SecureFileCollectionEntityJpa> findAll();

    @Override
    SecureFileCollectionEntityJpa findOneByDisplayName(String displayName);

    @Override
    SecureFileCollectionEntityJpa findOneByBucket(String bucket);

    @Override
    Long removeByPurgeInstantBefore(Instant cutOff);
}
