package ucles.weblab.common.files.domain;

/**
 * Persistence-technology-neutral interface representing a persistable secure file collection entity.
 *
 * @since 05/06/15
 */
public interface SecureFileCollectionEntity extends SecureFileCollection {
    String BUCKET_PREFIX = "fs.";

    String getId();

    String getBucket();

    /**
     * Derive a bucket name from the display name according to some rules.
     * Follows the conventions listed in http://stackoverflow.com/a/9868505.
     * <ul>
     * <li>lowercase</li>
     * <li>no word separators</li>
     * <li>begin with an underscore or letter</li>
     * <li>cannot contain $, NUL or be an empty string, cannot start with 'system.'</li>
     * </ul>
     *
     * @param displayName the display name selected by the user
     * @return the bucket name used as a unique id for the secure file storage
     */
    default String deriveBucket(String displayName) {
        final String safeName = displayName.toLowerCase().replaceAll("\\W", ""); // Replace all non-word chars
        return BUCKET_PREFIX + safeName;
    }


}
