package ucles.weblab.common.files.domain.jpa;

import org.springframework.data.repository.Repository;
import ucles.weblab.common.files.domain.SecureFileCollectionEntity;
import ucles.weblab.common.files.domain.SecureFileEntity;
import ucles.weblab.common.files.domain.SecureFileMetadataRepository;
import ucles.weblab.common.files.domain.SecureFileRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of a secure file metadata repository (read-only).
 *
 * @since 18/03/15
 */
public interface SecureFileMetadataRepositoryJpa extends SecureFileMetadataRepository, Repository<SecureFileMetadataEntityJpa, UUID> {
    @Override
    Optional<SecureFileMetadataEntityJpa> findOneByCollectionAndFilename(SecureFileCollectionEntity collection, String filename);

    @Override
    Collection<SecureFileMetadataEntityJpa> findAllByCollection(SecureFileCollectionEntity collection);
}
