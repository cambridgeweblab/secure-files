package ucles.weblab.common.files.domain;

/**
 * Value object (i.e. unidentified) representation of a secure file's metadata (not its content).
 * This parent interface allows operations which don't need a file's data to potentially work with a much smaller memory
 * footprint.
 *
 * @since 16/03/2016
 */
public interface SecureFileMetadata {
    String getFilename();

    String getContentType();

    long getLength();

    String getNotes();
}
