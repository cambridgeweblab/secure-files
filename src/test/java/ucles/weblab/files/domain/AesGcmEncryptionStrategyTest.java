package ucles.weblab.files.domain;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.KeyGenerator;

public class AesGcmEncryptionStrategyTest {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final AesGcmEncryptionStrategy aesGcmEncryptionStrategy = new AesGcmEncryptionStrategy("some-test-aad");
    private final byte[] randomSourceData = new byte[8192];

    private static final long SEED = 56275638975328L;

    {
        Random random = new Random(SEED);
        random.nextBytes(randomSourceData); // Generate 8K of random data to encrypt.
    }

    @Test
    public void testRoundTrippingAES_128() throws NoSuchAlgorithmException {
        Assert.assertTrue(aesGcmEncryptionStrategy.supports(EncryptionService.CIPHER_AES_GCM));
        final Provider[] providers = Security.getProviders();
        log.info("Providers: " + Arrays.asList(providers).toString());
        final Set<String> ciphers = new HashSet<>();
        for (Provider provider : providers) {
            ciphers.addAll(provider.getServices().stream()
                    .filter(service -> "Cipher".equals(service.getType()))
                    .map(Provider.Service::getAlgorithm)
                    .collect(Collectors.toList()));
        }
        log.info("Cipher algorithms: " + ciphers);
        final KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(128);
        byte[] key = keyGenerator.generateKey().getEncoded();
        byte[] encryptedData = aesGcmEncryptionStrategy.encrypt(EncryptionService.CIPHER_AES_GCM, key, randomSourceData);
        Assert.assertFalse(Arrays.equals(randomSourceData, encryptedData));
        byte[] outputData = aesGcmEncryptionStrategy.decrypt(EncryptionService.CIPHER_AES_GCM, key, encryptedData);
        Assert.assertArrayEquals(randomSourceData, outputData);
    }
}
