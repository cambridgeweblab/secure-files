package ucles.weblab.common.files.domain;

import java.time.Instant;
import ucles.weblab.common.domain.Buildable;

/**
 * Value object (i.e. unidentified) representation of a secure file.
 *
 * @since 05/06/15
 */
public interface SecureFile extends Buildable<SecureFile>, SecureFileMetadata {
    byte[] getPlainData();

    byte[] getEncryptedData();

    interface Builder extends Buildable.Builder<SecureFile> {
        Builder filename(String filename);
        Builder contentType(String contentType);
        Builder length(long length);
        Builder notes(String notes);
        Builder plainData(byte[] plainData);
        Builder createdDate(Instant createdDate);
    }
}
