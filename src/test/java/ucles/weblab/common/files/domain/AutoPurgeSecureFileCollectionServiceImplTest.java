package ucles.weblab.common.files.domain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @since 24/07/15
 */
@RunWith(MockitoJUnitRunner.class)
public class AutoPurgeSecureFileCollectionServiceImplTest {
    @Mock
    SecureFileCollectionRepository secureFileCollectionRepository;

    @InjectMocks
    private AutoPurgeSecureFileCollectionServiceImpl secureFileCollectionService;

    @Test
    public void testPurge() throws ExecutionException, InterruptedException {
        final Instant now = Instant.now();
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        when(secureFileCollectionRepository.removeByPurgeInstantBefore(now)).thenReturn(4L);

        secureFileCollectionService.configureClock(clock);
        final Future<Long> result = secureFileCollectionService.purgeRepository();
        assertTrue("Expect immediate completion", result.isDone());
        assertEquals("Expect count of deleted records", (Long) 4L, result.get());
    }
}
