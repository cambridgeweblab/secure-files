package ucles.weblab.files.domain.mongodb;

import org.springframework.data.repository.Repository;
import ucles.weblab.files.domain.SecureFileCollectionEntity;
import ucles.weblab.files.domain.SecureFileCollectionRepository;

import java.util.Collection;

/**
 * Implementation of a secure file collection repository.
 */
public interface SecureFileCollectionRepositoryMongo extends Repository<SecureFileCollectionEntityMongo, String>, SecureFileCollectionRepositoryMongoCustom, SecureFileCollectionRepository {
    @Override
    SecureFileCollectionEntityMongo findById(String id);

    @Override
    SecureFileCollectionEntityMongo save(SecureFileCollectionEntity s);

    @Override
    Collection<SecureFileCollectionEntityMongo> findAll();

    @Override
    SecureFileCollectionEntityMongo findOneByDisplayName(String displayName);

    @Override
    SecureFileCollectionEntityMongo findOneByBucket(String bucket);
}
