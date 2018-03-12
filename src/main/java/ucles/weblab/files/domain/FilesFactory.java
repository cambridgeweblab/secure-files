package ucles.weblab.files.domain;

/**
 * DDD factory interface, to create new entity objects.
 *
 * @since 05/06/15
 */
public interface FilesFactory {
    /**
     * Create a new collection and populate it with all the data from the value object.
     *
     * @param collection value object containing all required data for the collection
     * @return the newly-created entity, ready to persist
     */
    SecureFileCollectionEntity newSecureFileCollection(SecureFileCollection collection);

    /**
     * Create a new secure file with a collection and populate it with all the data from the value object.
     *
     * @param collection value object containing all required data for the file
     * @return the newly-created entity, ready to persist
     */
    SecureFileEntity newSecureFile(SecureFileCollectionEntity collection, SecureFile file);
}
