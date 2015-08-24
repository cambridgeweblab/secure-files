package ucles.weblab.common.files.domain.mongodb;

import org.springframework.data.repository.Repository;
import ucles.weblab.common.files.domain.SecureFileCollectionEntity;
import ucles.weblab.common.files.domain.SecureFileCollectionRepository;

import java.util.Collection;

/**
 * Implementation of a secure file collection repository.
 */
public interface SecureFileCollectionRepositoryMongo extends Repository<SecureFileCollectionEntityMongo, String>, SecureFileCollectionRepositoryMongoCustom, SecureFileCollectionRepository {
    @Override
    SecureFileCollectionEntityMongo findOne(String id);

    @Override
    SecureFileCollectionEntityMongo save(SecureFileCollectionEntity s);

    @Override
    Collection<SecureFileCollectionEntityMongo> findAll();

    @Override
    SecureFileCollectionEntityMongo findOneByDisplayName(String displayName);

    @Override
    SecureFileCollectionEntityMongo findOneByBucket(String bucket);
}
