package ucles.weblab.files.autoconfigure;

import de.flapdoodle.embed.mongo.MongodExecutable;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
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
@AutoConfigureAfter({MongoAutoConfiguration.class, MongoDataAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class})
@Import({MongoAutoConfiguration.class, MongoDataAutoConfiguration.class, FilesAutoConfiguration.class})
@ConditionalOnClass(FilesFactory.class)
//@ConditionalOnBean(MongoTemplate.class)
@ConditionalOnProperty(prefix = "spring.data.mongodb.repositories", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableMongoRepositories(basePackageClasses = {SecureFileCollectionRepositoryMongo.class})
public class FilesMongoAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(FilesFactoryMongo.class)
    public FilesFactoryMongo filesFactoryMongo() {
        return new FilesFactoryMongo();
    }

    @Bean
    @ConditionalOnMissingBean(SecureFileRepository.class)
    public SecureFileRepository secureFileRepositoryMongo(
            MongoOperations mongoOperations,
            GridFsOperations gridFsOperations,
            EncryptionService encryptionService
    ) {
        return new SecureFileRepositoryMongo(mongoOperations, gridFsOperations, encryptionService);
    }

    /**
     * Provide transaction manager if we're not using Flapdoodle Mongo database
     * Alternative would be test-containers for real Mongo
     */
    @Bean
    @ConditionalOnMissingBean({MongodExecutable.class})
    public MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    @Bean
    @ConditionalOnBean(MongodExecutable.class)
    public PlatformTransactionManager mongoTransactionManager() {
        return new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() throws TransactionException {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
            }

            @Override
            protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
            }

            @Override
            protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
            }
        };
    }
}
