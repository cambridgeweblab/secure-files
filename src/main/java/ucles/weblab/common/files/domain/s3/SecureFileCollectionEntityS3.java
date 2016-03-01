package ucles.weblab.common.files.domain.s3;

import java.time.Instant;
import java.util.Optional;
import ucles.weblab.common.files.blob.api.BlobStoreResult;
import ucles.weblab.common.files.domain.SecureFileCollectionEntity;

/**
 *
 * @author Sukhraj
 */
public class SecureFileCollectionEntityS3 implements SecureFileCollectionEntity {
    
    private final BlobStoreResult blobStoreResult;
    
    public SecureFileCollectionEntityS3(BlobStoreResult blobStoreResult) {
        this.blobStoreResult = blobStoreResult;
    }
    
    @Override
    public String getId() {
        return blobStoreResult.getBlobId().getId();
    }

    @Override
    public String getBucket() {
        return blobStoreResult.getRootPath();
    }

    @Override
    public String getDisplayName() {
        return blobStoreResult.getDisplayName();
    }

    @Override
    public Optional<Instant> getPurgeInstant() {
        return Optional.of(blobStoreResult.getPurgeInstant());
    }
    
}
