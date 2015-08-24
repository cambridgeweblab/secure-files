package ucles.weblab.common.files.domain;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.Random;
import javax.crypto.KeyGenerator;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class DummyEncryptionStrategyTest {
    private final DummyEncryptionStrategy dummyEncryptionStrategy = new DummyEncryptionStrategy();
    private final byte[] randomSourceData = new byte[8192];

    private static final long SEED = 632875632785632L;

    {
        Random random = new Random(SEED);
        random.nextBytes(randomSourceData); // Generate 8K of random data to encrypt.
    }

    @Test
    public void testRoundTrippingDummyStrategy() throws NoSuchAlgorithmException {
        assertTrue(dummyEncryptionStrategy.supports(EncryptionService.CIPHER_NONE));
        final KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        byte[] key = keyGenerator.generateKey().getEncoded();
        byte[] encryptedData = dummyEncryptionStrategy.encrypt(EncryptionService.CIPHER_NONE, key, randomSourceData);
        assertArrayEquals(randomSourceData, encryptedData);
        byte[] outputData = dummyEncryptionStrategy.decrypt(EncryptionService.CIPHER_NONE, key, encryptedData);
        assertArrayEquals(randomSourceData, outputData);
    }

}
