package ucles.weblab.common.files.domain.s3;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import ucles.weblab.common.files.blob.api.BlobId;
import ucles.weblab.common.files.blob.api.BlobStoreException;
import ucles.weblab.common.files.blob.api.BlobStoreResult;
import ucles.weblab.common.files.blob.api.BlobStoreService;
import ucles.weblab.common.files.domain.SecureFileCollectionEntity;
import ucles.weblab.common.files.domain.SecureFileCollectionRepository;


/**
 *
 * @author Sukhraj
 */
public class SecureFileCollectionRepositoryS3 implements SecureFileCollectionRepository {
    private static final String NOT_SUPPORTED_YET = "Not supported yet.";
    private final BlobStoreService blobStoreService;
    
    @Autowired
    public SecureFileCollectionRepositoryS3(BlobStoreService blobStoreService) {
        this.blobStoreService = blobStoreService;
    }
    
    @Override
    public SecureFileCollectionEntity findOne(String id) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * Save a collection into S3. A collection is a group of files. 
     * 
     * @param s
     * @return 
     */
    @Override
    public SecureFileCollectionEntity save(SecureFileCollectionEntity s) {
        String rootPath = s.getBucket();
        BlobId blobId = new BlobId(s.getId());
        byte[] data = new byte[1];
        
        try {
            Optional<BlobStoreResult> blobStoreResult = blobStoreService.putBlob(blobId, rootPath, data, s.getPurgeInstant().get());
            
            SecureFileCollectionEntityS3 entity = blobStoreResult.map(m -> new SecureFileCollectionEntityS3(m)).get();
            
            return entity;
        } catch (BlobStoreException ex) {
            Logger.getLogger(SecureFileCollectionRepositoryS3.class.getName()).log(Level.SEVERE, "BlobStoreException thrown while saving entity", ex);
        }
        return null;
    }

    @Override
    public Collection<? extends SecureFileCollectionEntity> findAll() {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SecureFileCollectionEntity findOneByDisplayName(String displayName) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SecureFileCollectionEntity findOneByBucket(String bucket) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Long removeByPurgeInstantBefore(Instant cutOff) {
        throw new UnsupportedOperationException(NOT_SUPPORTED_YET); //To change body of generated methods, choose Tools | Templates.
    }
    
}
