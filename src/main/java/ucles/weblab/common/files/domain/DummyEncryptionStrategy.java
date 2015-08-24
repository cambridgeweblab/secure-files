package ucles.weblab.common.files.domain;

/** A dummy implementation which doesn't actually encrypt or decrypt. */
public class DummyEncryptionStrategy implements EncryptionStrategy {
    @Override
    public boolean supports(String cipher) {
        return cipher.equals(EncryptionService.CIPHER_NONE);
    }

    @Override
    public byte[] encrypt(String cipherName, byte[] key, byte[] plainData) {
        return plainData;
    }

    @Override
    public byte[] decrypt(String cipher, byte[] key, byte[] encryptedData) {
        return encryptedData;
    }
}
