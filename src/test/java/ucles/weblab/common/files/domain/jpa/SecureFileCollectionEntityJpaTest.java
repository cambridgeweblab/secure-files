package ucles.weblab.common.files.domain.jpa;

import org.junit.Test;
import ucles.weblab.common.files.domain.SecureFileCollection;

import java.time.Instant;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SecureFileCollectionEntityJpaTest {
    @Test
    public void testBucketNameMeetsRequirements() {
        final String allKeyboardCharacters = "0123456789!@£$%^&*()-_+=±§{}[];:'\"\\|,<.>/?abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final String bucket = new SecureFileCollectionEntityJpa(new SecureFileCollection() {
            @Override
            public String getDisplayName() {
                return allKeyboardCharacters;
            }

            @Override
            public Optional<Instant> getPurgeInstant() {
                return Optional.empty();
            }
        }).getId();
        assertTrue("Should have no uppercase characters and be non-empty", bucket.matches("[^A-Z]+"));
        assertTrue("Should begins with underscore or letter", bucket.matches("[_a-z].*"));
        assertFalse("Should not contain $", bucket.contains("$"));
        assertFalse("Should not contain NUL", bucket.contains("\0"));
        assertFalse("Should not start with 'system.'", bucket.startsWith("system."));
    }
}
