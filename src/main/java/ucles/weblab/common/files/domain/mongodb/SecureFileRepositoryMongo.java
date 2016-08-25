package ucles.weblab.common.files.domain.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoOperations;
import ucles.weblab.common.files.domain.EncryptionService;
import ucles.weblab.common.files.domain.SecureFileCollectionEntity;
import ucles.weblab.common.files.domain.SecureFileEntity;
import ucles.weblab.common.files.domain.SecureFileRepository;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

/**
 * Implementation of a secure file repository.
 *
 * @since 18/03/15
 */
public class SecureFileRepositoryMongo implements SecureFileRepository {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final MongoOperations mongoOperations;
    private final EncryptionService encryptionService;
    private String cipherName;

    @Autowired
    public SecureFileRepositoryMongo(MongoOperations mongoOperations, EncryptionService encryptionService) {
        this.mongoOperations = mongoOperations;
        this.encryptionService = encryptionService;
    }

    @Autowired
    void configureCipher(@Value("${files.security.cipher:AES-GCM}") String cipherName) {
        this.cipherName = cipherName;
    }

    @Override
    public Optional<SecureFileEntityMongo> findOneByCollectionAndFilename(SecureFileCollectionEntity collection, String filename) {
        return mongoOperations.execute(db -> {
            final SecureFileCollectionEntityMongo bucket = (SecureFileCollectionEntityMongo) collection;
            GridFS gridFS = new GridFS(db, bucket.getBucket());
            return Optional.ofNullable(gridFS.findOne(filename)).map(f -> new SecureFileEntityMongo(bucket, f));
        });
    }

    @Override
    public SecureFileEntityMongo save(SecureFileEntity secureFile) {
        if (secureFile.isNew()) {
            return mongoOperations.execute(db -> {
                GridFS gridFS = null;
                final GridFSInputFile file;
                final SecureFileCollectionEntityMongo bucket = (SecureFileCollectionEntityMongo) secureFile.getCollection();
                gridFS = new GridFS(db, bucket.getBucket());
                ObjectId oid = new ObjectId();
                final byte[] encryptedData = encryptionService.encrypt(cipherName, oid.toByteArray(), secureFile.getPlainData());
                file = gridFS.createFile(new ByteArrayInputStream(encryptedData), secureFile.getFilename(), true);
                file.setId(oid);
                file.setContentType(secureFile.getContentType());
                file.put(SecureFileEntityMongo.CIPHER_PROPERTY, cipherName);
                file.put(SecureFileEntityMongo.NOTES_PROPERTY, secureFile.getNotes());
                file.save();

                return new SecureFileEntityMongo(bucket, gridFS.findOne((ObjectId) file.getId()));
            });
        } else {
            SecureFileEntityMongo mongoFile = (SecureFileEntityMongo) secureFile;
            return mongoOperations.execute(db -> {
                GridFS gridFS = null;
                final GridFSInputFile file;
                final SecureFileCollectionEntityMongo bucket = (SecureFileCollectionEntityMongo) secureFile.getCollection();
                gridFS = new GridFS(db, bucket.getBucket());

                final GridFSDBFile dbFile = mongoFile.getDbFile();
                dbFile.put(SecureFileEntityMongo.FILENAME_PROPERTY, secureFile.getFilename());
                dbFile.put(SecureFileEntityMongo.CONTENT_TYPE_PROPERTY, secureFile.getContentType());
                dbFile.put(SecureFileEntityMongo.NOTES_PROPERTY, secureFile.getNotes());
                dbFile.save();
                return mongoFile;
            });
        }
    }

    @Override
    public Collection<SecureFileEntityMongo> findAllByCollection(SecureFileCollectionEntity collection) {
        return mongoOperations.execute(db -> {
            final SecureFileCollectionEntityMongo bucket = (SecureFileCollectionEntityMongo) collection;
            GridFS gridFS = new GridFS(db, bucket.getBucket());
            // Can't use GridFS.getFileList() as it does not call GridFS._fix()
            // Have to instead use GridFS.find() which does.
            return gridFS.find(new BasicDBObject(), new BasicDBObject("filename", 1)).stream()
                    .map(f -> new SecureFileEntityMongo(bucket, f))
                    .collect(toList());
        });
    }

    @Override
    public void delete(SecureFileEntity file) {
        mongoOperations.execute(db -> {
            GridFS gridFS = null;
            final SecureFileCollectionEntityMongo bucket = (SecureFileCollectionEntityMongo) file.getCollection();
            gridFS = new GridFS(db, bucket.getBucket());
            gridFS.remove(file.getFilename());
            return null;
        });
    }

    @Override
    public Integer deleteByCollectionPurgeInstantBefore(Instant cutOff) {
        throw new UnsupportedOperationException("Purging not yet implemented for MongoDB.");
    }
}
