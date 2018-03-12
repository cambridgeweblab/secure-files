package ucles.weblab.files.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encryption with AES-GCM.
 * The encrypted data consists of a 16-byte random IV followed by the actual ciphertext. This ensures that a different
 * ciphertext will be produced even for the same key and plaintext.
 * @author Gareth Boden
 */
public class AesGcmEncryptionStrategy implements EncryptionStrategy {
    private static final Logger log = LoggerFactory.getLogger(AesGcmEncryptionStrategy.class);
    private static final boolean JDK8_PLUS = System.getProperty("java.specification.version").compareTo("1.8") >= 0;
    private static final int GCM_TAG_BITS = 128;

    /*Optional property string to update aad of the cipher */
    private String aad;

    public AesGcmEncryptionStrategy(String aad) {
        this.aad = aad;
    }

    static {
        // If we're on JDK < 8 then we need Bouncy Castle to implement AES-GCM. Otherwise it's built in.
        if (!JDK8_PLUS && Security.getProvider("BC") == null) {
            try {
                Security.addProvider((java.security.Provider) Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider").newInstance());
            } catch (Exception e) {
                throw new IllegalStateException("JDK <8 and no Bouncy Castle available.", e);
            }
        }
    }

    @Override
    public boolean supports(String cipher) {
        return cipher.equals(EncryptionService.CIPHER_AES_GCM);
    }

    @Override
    public byte[] encrypt(String cipherName, byte[] key, byte[] data) {
        try {
            Cipher cipher = createCipher();
            final SecureRandom random = new SecureRandom();
            final byte iv[] = new byte[cipher.getBlockSize()];
            random.nextBytes(iv);


            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), constructParamSpec(iv));
            if (aad != null) {
                cipher.updateAAD(aad.getBytes());
            }

            final byte[] buffer = new byte[cipher.getOutputSize(data.length)];
            final int outputSize = cipher.doFinal(data, 0, data.length, buffer);
            final byte[] encryptedData = new byte[iv.length + outputSize];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(buffer, 0, encryptedData, iv.length, outputSize);
            return encryptedData;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    @SuppressWarnings("UnnecessaryLocalVariable")
    public byte[] decrypt(String cipherName, byte[] key, byte[]encryptedData) {
        try {
            Cipher cipher = createCipher();
            final int blockSize = cipher.getBlockSize();
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), constructParamSpec(encryptedData, blockSize));

            if (aad != null) {
                cipher.updateAAD(aad.getBytes());
            }
            return cipher.doFinal(encryptedData, blockSize, encryptedData.length - blockSize);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    Cipher createCipher() throws GeneralSecurityException {
        return Cipher.getInstance("AES/GCM/NoPadding");
    }

    /**
     * Build an <code>AlgorithmParameterSpec</code> instance used to initialize a <code>Cipher</code> instance
     * for AES-GCM block cipher encryption. Uses IvParameterSpec below JDK8 and
     * GCMParameterSpec from JDK8 on.
     *
     * @param iv the initialization vector
     * @return the newly constructed AlgorithmParameterSpec instance, appropriate for the JDK version.
     */
    private AlgorithmParameterSpec constructParamSpec(byte[] iv) {
        if (JDK8_PLUS) {
            // Same as: new GCMParameterSpec(GCM_TAG_BITS, iv)
            log.trace("JDK8 or higher: Attempting to create GCMParameterSpec");
            try {
                Class gcmSpecClass = Class.forName("javax.crypto.spec.GCMParameterSpec");
                @SuppressWarnings("unchecked")
                AlgorithmParameterSpec gcmSpec = (AlgorithmParameterSpec) gcmSpecClass.getConstructor(int.class, byte[].class)
                        .newInstance(GCM_TAG_BITS, iv);
                log.trace("Successfully created GCMParameterSpec");
                return gcmSpec;
            } catch (Exception e) {
                log.debug("Failed to create GCMParameterSpec, falling back to returning IvParameterSpec", e);
                return new IvParameterSpec(iv);
            }

        } else {
            log.trace("JDK7 or below: returning IvParameterSpec");
            return new IvParameterSpec(iv);
        }
    }

    /**
     * Build an <code>AlgorithmParameterSpec</code> instance used to initialize a <code>Cipher</code> instance
     * for AES-GCM block cipher decryption. Requires JDK8+.Uses IvParameterSpec below JDK8 and GCMParameterSpec from JDK8 on.
     *
     * @param encryptedData the encrypted data
     * @param blockSize the block size (from Cipher)
     * @return the newly constructed AlgorithmParameterSpec instance, appropriate for the JDK version.
     */
    private AlgorithmParameterSpec constructParamSpec(byte[] encryptedData, int blockSize) {
        if (JDK8_PLUS) {
            // Same as: new GCMParameterSpec(GCM_TAG_BITS, encryptedData, 0, blockSize);
            log.trace("JDK8 or higher: Attempting to create GCMParameterSpec");
            try {
                Class gcmSpecClass = Class.forName("javax.crypto.spec.GCMParameterSpec");
                @SuppressWarnings("unchecked")
                AlgorithmParameterSpec gcmSpec = (AlgorithmParameterSpec) gcmSpecClass.getConstructor(int.class, byte[].class, int.class, int.class)
                        .newInstance(GCM_TAG_BITS, encryptedData, 0, blockSize);
                log.trace("Successfully created GCMParameterSpec");
                return gcmSpec;
            } catch (Exception e) {
                log.debug("Failed to create GCMParameterSpec, falling back to returning IvParameterSpec", e);
                return new IvParameterSpec(encryptedData, 0, blockSize);
            }

        } else {
            log.trace("JDK7 or below: returning IvParameterSpec");
            return new IvParameterSpec(encryptedData, 0, blockSize);
        }
    }
}
