package ucles.weblab.files.domain.mongodb;

import com.mongodb.BasicDBObject;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import ucles.weblab.files.domain.EncryptionService;
import ucles.weblab.files.domain.SecureFileCollectionEntity;
import ucles.weblab.files.domain.SecureFileEntity;
import ucles.weblab.files.domain.SecureFileRepository;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of a secure file repository.
 *
 * @since 18/03/15
 */
@RequiredArgsConstructor
public class SecureFileRepositoryMongo implements SecureFileRepository {
    private final MongoOperations mongoOperations;
    private final GridFsOperations gridFsOperations;
    private final EncryptionService encryptionService;
    private String cipherName;


    @Autowired
    void configureCipher(@Value("${files.security.cipher:AES-GCM}") String cipherName) {
        this.cipherName = cipherName;
    }

    @Override
    public Optional<SecureFileEntityMongo> findOneByCollectionAndFilename(SecureFileCollectionEntity collection, String filename) {
        final SecureFileCollectionEntityMongo bucket = (SecureFileCollectionEntityMongo) collection;
        return Optional.of(
            gridFsOperations.getResource(filename))
                .map(f -> {
                    try {
                        return f.exists() ? new SecureFileEntityMongo(bucket, f) : null;
                    } catch (IOException e) {
                        e.printStackTrace(); // FIXME
                        return null;
                    }
                });
}

    @SneakyThrows
    @Override
    public SecureFileEntityMongo save(SecureFileEntity secureFile) {

        if (secureFile.isNew()) {
            final SecureFileCollectionEntityMongo bucket = (SecureFileCollectionEntityMongo) secureFile.getCollection();
            ObjectId oid = new ObjectId();
            final byte[] encryptedData = encryptionService.encrypt(cipherName, oid.toByteArray(), secureFile.getPlainData());
            Document metadata = new Document();
            metadata.put(SecureFileEntityMongo.FILE_KEY, oid);
            metadata.put(SecureFileEntityMongo.CIPHER_PROPERTY, cipherName);
            metadata.put(SecureFileEntityMongo.NOTES_PROPERTY, secureFile.getNotes());
            //noinspection unused
            var objectId = gridFsOperations.store(new ByteArrayInputStream(encryptedData), secureFile.getFilename(), secureFile.getContentType(), metadata);

            var resource = Objects.requireNonNull(gridFsOperations.getResource(secureFile.getFilename()));
            return new SecureFileEntityMongo(bucket, resource);
        } else {
            SecureFileEntityMongo mongoFile = (SecureFileEntityMongo) secureFile;
            var metadata = Objects.requireNonNull(mongoFile.getDbFile().getMetadata());
            metadata.put(SecureFileEntityMongo.FILENAME_PROPERTY, secureFile.getFilename());
            metadata.put(SecureFileEntityMongo.CONTENT_TYPE_PROPERTY, secureFile.getContentType());
            metadata.put(SecureFileEntityMongo.NOTES_PROPERTY, secureFile.getNotes());
            mongoOperations.save(metadata);
            return mongoFile;
        }
    }

    @Override
    public Collection<SecureFileEntityMongo> findAllByCollection(SecureFileCollectionEntity collection) { // NOTE Bucket is ignored for search
        final SecureFileCollectionEntityMongo bucket = (SecureFileCollectionEntityMongo) collection;

        // get all sorted by filename
        var cursor = gridFsOperations.find(Query.query(Criteria.byExample(new BasicDBObject())).with(Sort.by(Sort.Order.asc("filename"))))
                .map(gridFSFile -> gridFsOperations.getResource(gridFSFile.getFilename()))
                .map(f -> {
                    try {
                        return new SecureFileEntityMongo(bucket, f);
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Oh hell");
                    }
                }).cursor();
        var results = new ArrayList<SecureFileEntityMongo>();
        cursor.forEachRemaining(results::add);
        return results;
    }

    @Override
    public void delete(SecureFileEntity file) {
        gridFsOperations.delete(Query.query(Criteria.where("_id").is(((SecureFileEntityMongo) file).getDbFile().getObjectId())));
    }

    @Override
    public Integer deleteByCollectionPurgeInstantBefore(Instant cutOff) {
        throw new UnsupportedOperationException("Purging not yet implemented for MongoDB.");
    }

}
