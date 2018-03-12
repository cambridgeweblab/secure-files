package ucles.weblab.files.domain;

/**
 * @since 18/03/15
 */
public interface EncryptionService {
    String CIPHER_NONE = "NONE";
    String CIPHER_AES_GCM = "AES-GCM";

    byte[] encrypt(String cipherName, byte[] fileKey, byte[] plainData);
    byte[] decrypt(String cipherName, byte[] fileKey, byte[] encryptedData);
}
