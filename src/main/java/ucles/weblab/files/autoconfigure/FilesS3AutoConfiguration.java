package ucles.weblab.files.autoconfigure;

import com.amazonaws.auth.BasicAWSCredentials;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ucles.weblab.files.blob.api.BlobStoreService;
import ucles.weblab.files.domain.s3.BlobStoreServiceS3;
import ucles.weblab.files.domain.s3.S3HealthCheckIndicator;

/**
 * An auto configuration class for when there is a BlobStoreServiceS3 initialised.
 *
 * @author Sukhraj
 */
@Configuration
@ConditionalOnBean({BlobStoreServiceS3.class})
public class FilesS3AutoConfiguration {

    /**
     * Declare health check for AWS S3
     * @param basicAWSCredentials
     * @param blobStoreService
     * @return
     */
    @Bean
    public S3HealthCheckIndicator healthCheckIndicator(BasicAWSCredentials basicAWSCredentials,
                                                       BlobStoreService blobStoreService) {

        return new S3HealthCheckIndicator(basicAWSCredentials, blobStoreService.getBucketName());
    }

}
