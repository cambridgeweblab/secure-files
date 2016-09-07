package ucles.weblab.common.files.domain.jpa;

import com.google.common.io.Resources;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import ucles.weblab.common.domain.ConfigurableEntitySupport;
import ucles.weblab.common.files.domain.*;
import ucles.weblab.common.files.webapi.converter.FilesConverters;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Root;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

/**
 * Tests that the secure file metadata repository works.
 *
 * @since 18/03/15
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration
@Transactional
public class SecureFileMetadataRepositoryJpa_IT {
    private static final String FILE_RESOURCE_PATH = "81672667_bus.jpg";

    @Configuration
    @EnableJpaRepositories(basePackageClasses = {SecureFileCollectionRepositoryJpa.class})
    @EntityScan(basePackageClasses = {SecureFileEntityJpa.class, Jsr310JpaConverters.class})
    @Import({ConfigurableEntitySupport.class, DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class, FilesConverters.class, FilesBuilders.class, PropertyPlaceholderAutoConfiguration.class})
    public static class Config {
        @Bean
        public FilesFactory filesFactoryJpa() {
            return new FilesFactoryJpa();
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
    FilesFactoryJpa filesFactory;

    @Autowired
    SecureFileRepositoryJpa secureFileRepository;

    @Autowired
    SecureFileMetadataRepositoryJpa secureFileMetadataRepository;

    @Autowired
    SecureFileCollectionService secureFileCollectionService;

    @PersistenceContext
    EntityManager entityManager;

    private SecureFileCollectionEntity bucket;

    @Before
    public void createBucket() {
        bucket = filesFactory.newSecureFileCollection(new SecureFileCollection() {
            @Override
            public String getDisplayName() {
                return UUID.randomUUID().toString();
            }

            @Override
            public Optional<Instant> getPurgeInstant() {
                return Optional.of(Instant.now().minusMillis(1L));
            }
        });
        entityManager.persist(bucket);
    }

    @After
    public void emptyBucket() {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaDelete<SecureFileEntityJpa> delete = cb.createCriteriaDelete(SecureFileEntityJpa.class);
        final Root<SecureFileEntityJpa> e = delete.from(SecureFileEntityJpa.class);
        delete.where(cb.equal(e.get(SecureFileEntityJpa_.collection).get(SecureFileCollectionEntityJpa_.bucket), bucket.getId()));
        entityManager.createQuery(delete).executeUpdate();
    }

    private SecureFileEntityJpa newSecureFile(final byte[] originalData) {
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
                
                @Override
                public Instant getCreatedDate() {
                    return Instant.now();
                }
            });
    }

    @Transactional
    @Test
    public void testFileMetadataUpdate() throws IOException {
        final byte[] originalData = Resources.toByteArray(getClass().getResource(FILE_RESOURCE_PATH));
        final SecureFileEntity newFile = newSecureFile(originalData);
        final SecureFileEntityJpa secureFile = secureFileRepository.save(newFile);
        assertEquals("Expected saved file to have original notes", "Very important document", secureFile.getNotes());

        secureFile.setNotes("Very very important document");
        final SecureFileEntityJpa updatedFile = secureFileRepository.save(secureFile);
        final SecureFileMetadataEntityJpa reloadedFile = secureFileMetadataRepository.findOneByCollectionAndFilename(bucket, secureFile.getFilename()).get();
        assertEquals("Expect found file to have updated notes", "Very very important document", reloadedFile.getNotes());
    }

}
