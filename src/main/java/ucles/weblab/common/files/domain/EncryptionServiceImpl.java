package ucles.weblab.common.files.domain;

import java.util.Collection;

/**
 * Provides in-memory encryption and decryption services for files, with multiple cipher implementations.
 *
 * @since 18/03/15
 */
public class EncryptionServiceImpl implements EncryptionService {
    private final Collection<EncryptionStrategy> delegates;
    private byte[] secretKey;

    public EncryptionServiceImpl(Collection<EncryptionStrategy> delegates, byte[] secretKey) {
        this.delegates = delegates;
        this.secretKey = secretKey;
    }

    @Override
    public byte[] encrypt(String cipherName, byte[] fileKey, byte[] plainData) {
        byte[] combinedKey = combineKeys(secretKey, fileKey);

        for (EncryptionStrategy delegate : delegates) {
            if (delegate.supports(cipherName)) {
                return delegate.encrypt(cipherName, combinedKey, plainData);
            }
        }
        throw new IllegalArgumentException("No encryption strategy known for: " + cipherName);
    }

    @Override
    public byte[] decrypt(String cipherName, byte[] fileKey, byte[] encryptedData) {
        byte[] combinedKey = combineKeys(secretKey, fileKey);

        for (EncryptionStrategy delegate : delegates) {
            if (delegate.supports(cipherName)) {
                return delegate.decrypt(cipherName, combinedKey, encryptedData);
            }
        }
        throw new IllegalArgumentException("No encryption strategy known for: " + cipherName);
    }

    private byte[] combineKeys(byte[] secretKey, byte[] fileKey) {
        if (fileKey.length == 0) {
            return secretKey;
        }

        byte[] output = new byte[secretKey.length];
        for (int i = 0; i < output.length; i++) {
            output[i] = (byte) (secretKey[i] ^ fileKey[i % fileKey.length]);
        }
        return output;
    }
}
