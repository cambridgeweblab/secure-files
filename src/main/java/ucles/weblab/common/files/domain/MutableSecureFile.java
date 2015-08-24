package ucles.weblab.common.files.domain;

/**
 * This class is ...
 *
 * @since 02/07/15
 */
public interface MutableSecureFile extends SecureFile {
    void setFilename(String filename);

    void setContentType(String contentType);

    void setNotes(String notes);
}
