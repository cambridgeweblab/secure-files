package ucles.weblab.files.autoconfigure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import ucles.weblab.files.domain.*;
import ucles.weblab.files.webapi.FileController;
import ucles.weblab.files.webapi.converter.FilesConverters;

import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Auto-configuration for the files domain.
 */
@Configuration
@ConditionalOnClass({EncryptionService.class, SecureFileCollectionRepository.class})
//@AutoConfigureAfter({FilesJpaAutoConfiguration.class, FilesS3AutoConfiguration.class})
@Import({FilesConverters.class, FilesBuilders.class})
@ComponentScan(basePackageClasses = FileController.class)
public class FilesAutoConfiguration {

    /*Optional property for AesGcmEncryptionStrategy*/
    @Value("${aad.string:}")
    private String aadString;

    @Bean
    @ConditionalOnProperty("files.security.secretkey")
    @ConditionalOnMissingBean(EncryptionService.class)
    public EncryptionService encryptionService(@Value("${files.security.secretkey}") String secretKey) {
        return new EncryptionServiceImpl(Arrays.asList(new AesGcmEncryptionStrategy(aadString.isEmpty() ? null : aadString),
                                        new DummyEncryptionStrategy()),
                                        secretKey.getBytes(UTF_8));
    }

    @Bean
    @ConditionalOnMissingBean(SecureFileCollectionService.class)
    public SecureFileCollectionService secureFileCollectionService(SecureFileCollectionRepository secureFileCollectionRepository, SecureFileRepository secureFileRepository) {
        return new AutoPurgeSecureFileCollectionServiceImpl(secureFileCollectionRepository, secureFileRepository);
    }
}
