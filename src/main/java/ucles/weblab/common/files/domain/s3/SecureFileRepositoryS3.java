package ucles.weblab.common.files.domain.s3;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import ucles.weblab.common.files.domain.SecureFileCollectionEntity;
import ucles.weblab.common.files.domain.SecureFileEntity;
import ucles.weblab.common.files.domain.SecureFileRepository;

/**
 *
 * @author Sukhraj
 */
public class SecureFileRepositoryS3 implements SecureFileRepository {

    private final BlobStoreServiceS3 blobStoreService;
    
    @Autowired
    public SecureFileRepositoryS3(BlobStoreServiceS3 blobStoreService) {
        this.blobStoreService = blobStoreService;
    }
    
    @Override
    public Optional<? extends SecureFileEntity> findOneByCollectionAndFilename(SecureFileCollectionEntity collection, String filename) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public SecureFileEntity save(SecureFileEntity secureFile) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Collection<? extends SecureFileEntity> findAllByCollection(SecureFileCollectionEntity collection) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void delete(SecureFileEntity file) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Integer deleteByCollectionPurgeInstantBefore(Instant cutOff) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
