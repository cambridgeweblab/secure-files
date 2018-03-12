package ucles.weblab.files.domain;

public interface EncryptionStrategy {
    boolean supports(String cipher);

    byte[] decrypt(@SuppressWarnings("UnusedParameters") String cipherName, byte[] key, byte[] encryptedData);

    byte[] encrypt(@SuppressWarnings("UnusedParameters") String cipherName, byte[] key, byte[] plainData);
}
