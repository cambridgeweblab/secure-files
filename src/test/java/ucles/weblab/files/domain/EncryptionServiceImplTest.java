package ucles.weblab.files.domain;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EncryptionServiceImplTest {
    @Mock
    EncryptionStrategy mockEncryptionStrategy;
    @Captor
    ArgumentCaptor<byte[]> keyCaptor;

    private EncryptionServiceImpl encryptionService1;
    private EncryptionServiceImpl encryptionService2;

    @Before
    public void setUp() {
        encryptionService1 = new EncryptionServiceImpl(Collections.singleton(mockEncryptionStrategy), "SecretKey1".getBytes(UTF_8));
        encryptionService2 = new EncryptionServiceImpl(Collections.singleton(mockEncryptionStrategy), "SecretKey2".getBytes(UTF_8));
    }

    @Test
    public void testEncryptionStrategyMatched() {
        final byte[] plainData = "Sweetheart".getBytes();
        final byte[] encryptedData = "Savoy".getBytes();
        when(mockEncryptionStrategy.supports("Cabbage")).thenReturn(true);
        when(mockEncryptionStrategy.encrypt(eq("Cabbage"), any(), eq(plainData))).thenReturn(encryptedData);

        final byte[] result = encryptionService1.encrypt("Cabbage", new byte[0], plainData);
        assertArrayEquals("Encryption strategy matched", encryptedData, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncryptionStrategyNotMatched() {
        final byte[] plainData = "Sweetheart".getBytes();
        when(mockEncryptionStrategy.supports("Cabbage")).thenReturn(false);

        encryptionService1.encrypt("Cabbage", new byte[0], plainData);
    }

    @Test
    public void testFileKeyVariesEncryptionKey() {
        final byte[] plainData = "Sweetheart".getBytes();
        final byte[] encryptedData = "Savoy".getBytes();
        when(mockEncryptionStrategy.supports("Cabbage")).thenReturn(true);
        when(mockEncryptionStrategy.encrypt(eq("Cabbage"), any(), eq(plainData))).thenReturn(encryptedData);

        encryptionService1.encrypt("Cabbage", "SmallKey".getBytes(), plainData);
        encryptionService1.encrypt("Cabbage", "VeryLongKey".getBytes(), plainData);

        verify(mockEncryptionStrategy, times(2)).encrypt(eq("Cabbage"), keyCaptor.capture(), eq(plainData));

        final List<byte[]> allValues = keyCaptor.getAllValues();
        assertFalse("Expected different keys", Arrays.equals(allValues.get(0), allValues.get(1)));
    }

    @Test
    public void testSecretKeyVariesEncryptionKey() {
        final byte[] plainData = "Sweetheart".getBytes();
        final byte[] encryptedData = "Savoy".getBytes();
        when(mockEncryptionStrategy.supports("Cabbage")).thenReturn(true);
        when(mockEncryptionStrategy.encrypt(eq("Cabbage"), any(), eq(plainData))).thenReturn(encryptedData);

        encryptionService1.encrypt("Cabbage", "FileKey".getBytes(), plainData);
        encryptionService2.encrypt("Cabbage", "FileKey".getBytes(), plainData);

        verify(mockEncryptionStrategy, times(2)).encrypt(eq("Cabbage"), keyCaptor.capture(), eq(plainData));

        final List<byte[]> allValues = keyCaptor.getAllValues();
        assertFalse("Expected different keys", Arrays.equals(allValues.get(0), allValues.get(1)));
    }

    @Test
    public void testDecryptionStrategyMatched() {
        final byte[] plainData = "Sweetheart".getBytes();
        final byte[] decryptedData = "Savoy".getBytes();
        when(mockEncryptionStrategy.supports("Cabbage")).thenReturn(true);
        when(mockEncryptionStrategy.decrypt(eq("Cabbage"), any(), eq(plainData))).thenReturn(decryptedData);

        final byte[] result = encryptionService1.decrypt("Cabbage", new byte[0], plainData);
        assertArrayEquals("Decryption strategy matched", decryptedData, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDecryptionStrategyNotMatched() {
        final byte[] plainData = "Sweetheart".getBytes();
        when(mockEncryptionStrategy.supports("Cabbage")).thenReturn(false);

        encryptionService1.decrypt("Cabbage", new byte[0], plainData);
    }

    @Test
    public void testFileKeyVariesDecryptionKey() {
        final byte[] plainData = "Sweetheart".getBytes();
        final byte[] decryptedData = "Savoy".getBytes();
        when(mockEncryptionStrategy.supports("Cabbage")).thenReturn(true);
        when(mockEncryptionStrategy.decrypt(eq("Cabbage"), any(), eq(plainData))).thenReturn(decryptedData);

        encryptionService1.decrypt("Cabbage", "SmallKey".getBytes(), plainData);
        encryptionService1.decrypt("Cabbage", "VeryLongKey".getBytes(), plainData);

        verify(mockEncryptionStrategy, times(2)).decrypt(eq("Cabbage"), keyCaptor.capture(), eq(plainData));

        final List<byte[]> allValues = keyCaptor.getAllValues();
        assertFalse("Expected different keys", Arrays.equals(allValues.get(0), allValues.get(1)));
    }

    @Test
    public void testSecretKeyVariesDecryptionKey() {
        final byte[] plainData = "Sweetheart".getBytes();
        final byte[] decryptedData = "Savoy".getBytes();
        when(mockEncryptionStrategy.supports("Cabbage")).thenReturn(true);
        when(mockEncryptionStrategy.decrypt(eq("Cabbage"), any(), eq(plainData))).thenReturn(decryptedData);

        encryptionService1.decrypt("Cabbage", "FileKey".getBytes(), plainData);
        encryptionService2.decrypt("Cabbage", "FileKey".getBytes(), plainData);

        verify(mockEncryptionStrategy, times(2)).decrypt(eq("Cabbage"), keyCaptor.capture(), eq(plainData));

        final List<byte[]> allValues = keyCaptor.getAllValues();
        assertFalse("Expected different keys", Arrays.equals(allValues.get(0), allValues.get(1)));
    }

}
