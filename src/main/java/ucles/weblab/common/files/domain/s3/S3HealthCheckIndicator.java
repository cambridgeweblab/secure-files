package ucles.weblab.common.files.domain.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetBucketLocationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

/**
 * A healthcheck indicator to test connectivity with Amazon S3. 
 * This will get the location of the bucket. If any exceptions occur, then it will 
 * mark the health as DOWN. The bucket should already exist for this health 
 * check indicator to be of real use. 
 * 
 * @author Sukhraj
 */
public class S3HealthCheckIndicator implements HealthIndicator{

    private static final Logger log = LoggerFactory.getLogger(BlobStoreServiceS3.class);
    
    /**Store the credentials*/
    private final BasicAWSCredentials awsCredentials;  
    
    /**The bucket name that is living under s3 */
    private final String bucketName;
        
    public S3HealthCheckIndicator(BasicAWSCredentials awsCredentials,
                                  String bucketName) {
        this.awsCredentials = awsCredentials;
        this.bucketName = bucketName;
    }
    
    @Override
    public Health health() {
        
        AmazonS3Client s3Client = new AmazonS3Client(awsCredentials);
        String currentLocation = null;
        try {
            GetBucketLocationRequest request = new GetBucketLocationRequest(bucketName);
            //get the bucket location to test if connection works
            currentLocation = s3Client.getBucketLocation(request);
        } catch (AmazonClientException a) {
            
            log.warn("{} caught while getting bucket location for: {}", a.getClass().getSimpleName(), bucketName, a);
            return Health.down().withDetail("Amazon S3 Request Exception", a.getMessage()).build();
        }
        //return up if we get here
        return Health.up().build();
    }
    
}
