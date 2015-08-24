package ucles.weblab.common.files.domain.mongodb;

import ucles.weblab.common.files.domain.FilesFactory;
import ucles.weblab.common.files.domain.SecureFile;
import ucles.weblab.common.files.domain.SecureFileCollection;
import ucles.weblab.common.files.domain.SecureFileCollectionEntity;
import ucles.weblab.common.files.domain.SecureFileEntity;
import ucles.weblab.common.files.domain.jpa.SecureFileCollectionRepositoryJpa;

/**
 * Implementation of the factory interface which creates MongoDB entities, suitable for persistence with
 * {@link SecureFileCollectionRepositoryJpa#save(SecureFileCollectionEntity)} and
 * {@link SecureFileRepositoryMongo#save(SecureFileEntity)}.
 *
 * @since 05/06/15
 *
 * @since 05/06/15
 */
public class FilesFactoryMongo implements FilesFactory {
    @Override
    public SecureFileCollectionEntityMongo newSecureFileCollection(SecureFileCollection collection) {
        return new SecureFileCollectionEntityMongo(collection);
    }

    @Override
    public SecureFileEntity newSecureFile(SecureFileCollectionEntity collection, SecureFile file) {
        return new ImmutableSecureFileEntityAdapter(collection, file);
    }

    private static class ImmutableSecureFileEntityAdapter implements SecureFileEntity {
        private final SecureFileCollectionEntity collection;
        private final SecureFile file;

        public ImmutableSecureFileEntityAdapter(SecureFileCollectionEntity collection, SecureFile file) {
            this.collection = collection;
            this.file = file;
        }

        @Override
        public SecureFileCollectionEntity getCollection() {
            return collection;
        }

        @Override
        public boolean isNew() {
            return true;
        }

        @Override
        public String getFilename() {
            return file.getFilename();
        }

        @Override
        public String getContentType() {
            return file.getContentType();
        }

        @Override
        public long getLength() {
            return file.getLength();
        }

        @Override
        public String getNotes() {
            return file.getNotes();
        }

        @Override
        public byte[] getPlainData() {
            return file.getPlainData();
        }

        @Override
        public void setFilename(String filename) {
            throw new UnsupportedOperationException("Immutable entity adapter");
        }

        @Override
        public void setContentType(String contentType) {
            throw new UnsupportedOperationException("Immutable entity adapter");
        }

        @Override
        public void setNotes(String notes) {
            throw new UnsupportedOperationException("Immutable entity adapter");
        }
    }
}
