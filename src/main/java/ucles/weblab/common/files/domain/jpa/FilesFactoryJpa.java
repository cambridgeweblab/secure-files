package ucles.weblab.common.files.domain.jpa;

import ucles.weblab.common.files.domain.FilesFactory;
import ucles.weblab.common.files.domain.SecureFile;
import ucles.weblab.common.files.domain.SecureFileCollection;
import ucles.weblab.common.files.domain.SecureFileCollectionEntity;
import ucles.weblab.common.files.domain.SecureFileEntity;

/**
 * Implementation of the factory interface which creates JPA entities, suitable for persistence with
 * {@link SecureFileCollectionRepositoryJpa#save(SecureFileCollectionEntity)} and
 * {@link SecureFileRepositoryJpa#save(SecureFileEntity)}.
 *
 * @since 05/06/15
 */
public class FilesFactoryJpa implements FilesFactory {
    @Override
    public SecureFileCollectionEntityJpa newSecureFileCollection(SecureFileCollection collection) {
        return new SecureFileCollectionEntityJpa(collection);
    }

    @Override
    public SecureFileEntityJpa newSecureFile(SecureFileCollectionEntity collection, SecureFile file) {
        return new SecureFileEntityJpa((SecureFileCollectionEntityJpa) collection, file);
    }
}
