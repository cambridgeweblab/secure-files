package ucles.weblab.common.files.domain;

import ucles.weblab.common.domain.Buildable;

/**
 * Value object (i.e. unidentified) representation of a secure file.
 *
 * @since 05/06/15
 */
public interface SecureFile extends Buildable<SecureFile> {
    String getFilename();
    String getContentType();
    long getLength();
    String getNotes();
    byte[] getPlainData();

    interface Builder extends Buildable.Builder<SecureFile> {
        Builder filename(String filename);
        Builder contentType(String contentType);
        Builder length(long length);
        Builder notes(String notes);
        Builder plainData(byte[] plainData);
    }
}
