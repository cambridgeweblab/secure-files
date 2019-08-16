package ucles.weblab.files.domain.jpa;

import ucles.weblab.files.domain.FilesFactory;
import ucles.weblab.files.domain.SecureFile;
import ucles.weblab.files.domain.SecureFileCollection;
import ucles.weblab.files.domain.SecureFileCollectionEntity;

/**
 * Implementation of the factory interface which creates JPA entities, suitable for persistence with
 * {@link SecureFileCollectionRepositoryJpa#save(SecureFileCollectionEntity)} and
 * {@link SecureFileRepositoryJpa#save(ucles.weblab.files.domain.SecureFileEntity)}.
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
