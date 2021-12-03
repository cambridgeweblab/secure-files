package ucles.weblab.files.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import ucles.weblab.files.domain.EncryptionService;
import ucles.weblab.files.domain.FilesFactory;
import ucles.weblab.files.domain.SecureFileRepository;
import ucles.weblab.files.domain.mongodb.FilesFactoryMongo;
import ucles.weblab.files.domain.mongodb.SecureFileCollectionRepositoryMongo;
import ucles.weblab.files.domain.mongodb.SecureFileRepositoryMongo;

/**
 * Auto-configuration for MongoDB support for file storage.
 *
 * @since 19/06/15
 */
@Configuration
@AutoConfigureAfter({MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
@ConditionalOnClass(FilesFactory.class)
@ConditionalOnBean(MongoOperations.class)
@ConditionalOnProperty(prefix = "spring.data.mongodb.repositories", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableMongoRepositories(basePackageClasses = {SecureFileCollectionRepositoryMongo.class})
public class FilesMongoAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(FilesFactory.class)
    public FilesFactory filesFactoryMongo() {
        return new FilesFactoryMongo();
    }

    @Bean
    @ConditionalOnMissingBean(SecureFileRepository.class)
    public SecureFileRepository secureFileRepositoryMongo(MongoOperations mongoOperations, GridFsOperations gridFsOperations, EncryptionService encryptionService) {
        return new SecureFileRepositoryMongo(mongoOperations, gridFsOperations, encryptionService);
    }
}
