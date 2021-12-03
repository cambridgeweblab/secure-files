package ucles.weblab.files.domain.mongodb;

import com.google.common.io.Resources;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import ucles.weblab.files.domain.AesGcmEncryptionStrategy;
import ucles.weblab.files.domain.AutoPurgeSecureFileCollectionServiceImpl;
import ucles.weblab.files.domain.DummyEncryptionStrategy;
import ucles.weblab.files.domain.EncryptionService;
import ucles.weblab.files.domain.EncryptionServiceImpl;
import ucles.weblab.files.domain.FilesBuilders;
import ucles.weblab.files.domain.FilesFactory;
import ucles.weblab.files.domain.SecureFile;
import ucles.weblab.files.domain.SecureFileCollection;
import ucles.weblab.files.domain.SecureFileCollectionRepository;
import ucles.weblab.files.domain.SecureFileCollectionService;
import ucles.weblab.files.domain.SecureFileEntity;
import ucles.weblab.files.domain.SecureFileRepository;
import ucles.weblab.files.webapi.converter.FilesConverters;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tests that the secure file repository works.
 *
 * @since 18/03/15
 */
@ExtendWith(SpringExtension.class)
@DataMongoTest
//@Transactional
public class SecureFileRepositoryMongo_IT {
    private static final String FILE_RESOURCE_PATH = "81672667_bus.jpg";

    @Configuration
    @EnableMongoRepositories(basePackageClasses = {SecureFileCollectionRepositoryMongo.class})
    @Import({MongoAutoConfiguration.class, MongoDataAutoConfiguration.class, FilesConverters.class, FilesBuilders.class, PropertyPlaceholderAutoConfiguration.class})
    public static class Config {
        @Bean
        public FilesFactory filesFactoryMongo() {
            return new FilesFactoryMongo();
        }

        @Bean
        public SecureFileRepository secureFileRepositoryMongo(MongoOperations mongoOperations, GridFsOperations gridFsOperations, EncryptionService encryptionService) {
            return new SecureFileRepositoryMongo(mongoOperations, gridFsOperations, encryptionService);
        }

        @Bean
        public EncryptionService encryptionService() {
            return new EncryptionServiceImpl(Arrays.asList(new AesGcmEncryptionStrategy("some-test-aad"), new DummyEncryptionStrategy()),
                    "0123456789012345".getBytes(UTF_8));
        }

        @Bean
        public SecureFileCollectionService secureFileCollectionService(SecureFileCollectionRepository secureFileCollectionRepository, SecureFileRepository secureFileRepository) {
            return new AutoPurgeSecureFileCollectionServiceImpl(secureFileCollectionRepository, secureFileRepository);
        }
    }

    @Autowired
    FilesFactoryMongo filesFactory;

    @Autowired
    SecureFileRepositoryMongo secureFileRepository;

    @Autowired
    MongoTemplate template;

    private SecureFileCollectionEntityMongo bucket;

//    @Before
    public void createBucket() {
        bucket = filesFactory.newSecureFileCollection(new SecureFileCollection() {
            @Override
            public String getDisplayName() {
                return SecureFileRepositoryMongo_IT.class.getSimpleName();
            }

            @Override
            public Optional<Instant> getPurgeInstant() {
                return Optional.of(Instant.now());
            }
        });
    }

//    @After
    public void emptyBucket() {
        template.dropCollection(bucket.getBucket() + ".chunks");
        template.dropCollection(bucket.getBucket() + ".files");
    }

    @Transactional
    @Test
//    @Ignore
    public void testSecureFileRoundTrip() throws IOException {
        final byte[] originalData = Resources.toByteArray(getClass().getResource(FILE_RESOURCE_PATH));
        final SecureFileEntity newFile = newSecureFile(originalData);
        final SecureFileEntityMongo secureFile = secureFileRepository.save(newFile);
        final byte[] encryptedData = secureFile.getEncryptedData();
        final byte[] decryptedData = secureFile.getPlainData();
        assertFalse("Expected different keys", Arrays.equals(originalData, encryptedData));
        assertArrayEquals("Decrypted content should match", originalData, decryptedData);

        final Collection<SecureFileEntityMongo> allByBucket = secureFileRepository.findAllByCollection(bucket);
        assertThat(allByBucket).contains(secureFile);

        secureFileRepository.delete(secureFile);
        assertThat(secureFileRepository.findAllByCollection(bucket)).doesNotContain(secureFile);
    }

    private SecureFileEntity newSecureFile(final byte[] originalData) {
        return filesFactory.newSecureFile(bucket, new SecureFile() {
                @Override
                public String getFilename() {
                    return FILE_RESOURCE_PATH;
                }

                @Override
                public String getContentType() {
                    return "image/jpeg";
                }

                @Override
                public String getNotes() {
                    return "Very important document";
                }

                @Override
                public long getLength() {
                    return originalData.length;
                }

                @Override
                public byte[] getEncryptedData() { return originalData; }

                @Override
                public byte[] getPlainData() {
                    return originalData;
                }

                @Override
                public Instant getCreatedDate() {
                    return Instant.now();
                }
            });
    }

    @Transactional
    @Test
    @Ignore
    public void testFileMetadataUpdate() throws IOException {
        final byte[] originalData = Resources.toByteArray(getClass().getResource(FILE_RESOURCE_PATH));
        final SecureFileEntity newFile = newSecureFile(originalData);
        final SecureFileEntityMongo secureFile = secureFileRepository.save(newFile);
        assertEquals("Expected saved file to have original notes", "Very important document", secureFile.getNotes());

        secureFile.setNotes("Very very important document");
        secureFileRepository.save(secureFile);
        final SecureFileEntityMongo reloadedFile = secureFileRepository.findOneByCollectionAndFilename(bucket, secureFile.getFilename()).get();
        assertEquals("Expect found file to have updated notes", "Very very important document", reloadedFile.getNotes());
    }

}
