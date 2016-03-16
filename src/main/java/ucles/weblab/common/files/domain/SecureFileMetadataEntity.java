package ucles.weblab.common.files.domain;

/**
 * Persistence-technology-neutral interface representing a persistable secure file's metadata only, in a collection.
 * This allows operations which don't need a file's data to work with a much smaller memory footprint.
 *
 * @since 16/03/2016
 */
public interface SecureFileMetadataEntity extends SecureFileMetadata {
    SecureFileCollectionEntity getCollection();
}
