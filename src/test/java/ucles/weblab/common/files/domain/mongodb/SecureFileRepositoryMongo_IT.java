package ucles.weblab.common.files.domain.mongodb;

import com.google.common.io.Resources;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import ucles.weblab.common.files.domain.AesGcmEncryptionStrategy;
import ucles.weblab.common.files.domain.AutoPurgeSecureFileCollectionServiceImpl;
import ucles.weblab.common.files.domain.DummyEncryptionStrategy;
import ucles.weblab.common.files.domain.EncryptionService;
import ucles.weblab.common.files.domain.EncryptionServiceImpl;
import ucles.weblab.common.files.domain.FilesBuilders;
import ucles.weblab.common.files.domain.FilesFactory;
import ucles.weblab.common.files.domain.SecureFile;
import ucles.weblab.common.files.domain.SecureFileCollection;
import ucles.weblab.common.files.domain.SecureFileCollectionRepository;
import ucles.weblab.common.files.domain.SecureFileCollectionService;
import ucles.weblab.common.files.domain.SecureFileEntity;
import ucles.weblab.common.files.domain.SecureFileRepository;
import ucles.weblab.common.files.webapi.FileController;
import ucles.weblab.common.files.webapi.converter.FilesConverters;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

/**
 * Tests that the secure file repository works.
 *
 * @since 18/03/15
 */
@Ignore("MongoDB not available in the environment") // TODO: make this detectable
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
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
        public SecureFileRepository secureFileRepositoryMongo(MongoOperations mongoOperations, EncryptionService encryptionService) {
            return new SecureFileRepositoryMongo(mongoOperations, encryptionService);
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
    @Ignore
    public void testSecureFileRoundTrip() throws IOException {
        final byte[] originalData = Resources.toByteArray(getClass().getResource(FILE_RESOURCE_PATH));
        final SecureFileEntity newFile = newSecureFile(originalData);
        final SecureFileEntityMongo secureFile = secureFileRepository.save(newFile);
        final byte[] encryptedData = secureFile.getEncryptedData();
        final byte[] decryptedData = secureFile.getPlainData();
        assertFalse("Expected different keys", Arrays.equals(originalData, encryptedData));
        assertArrayEquals("Decrypted content should match", originalData, decryptedData);

        final Collection<SecureFileEntityMongo> allByBucket = secureFileRepository.findAllByCollection(bucket);
        assertThat(allByBucket, contains(secureFile));

        secureFileRepository.delete(secureFile);
        assertThat(secureFileRepository.findAllByCollection(bucket), not(contains(secureFile)));
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
                public byte[] getPlainData() {
                    return originalData;
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
