package ucles.weblab.common.files.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.Scheduled;

import javax.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.Future;

/**
 * Implementation of the service which as well as providing an on-demand purge also schedules an automatic purge
 * of expired file collections every day.
 *
 * @since 24/07/15
 */
@Transactional
public class AutoPurgeSecureFileCollectionServiceImpl implements SecureFileCollectionService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Clock clock = Clock.systemUTC();

    public AutoPurgeSecureFileCollectionServiceImpl(SecureFileCollectionRepository secureFileCollectionRepository) {
        this.secureFileCollectionRepository = secureFileCollectionRepository;
    }

    private final SecureFileCollectionRepository secureFileCollectionRepository;

    @Autowired(required = false) // will fall back to default system UTC clock
    void configureClock(Clock clock) {
        logger.warn("Clock overridden with " + clock);
        this.clock = clock;
    }

    /**
     * Scheduled job to purge the repositories once a day at 4am (or override with {@code files.purge.cron}).
     */
    @Override
    @Scheduled(cron = "${files.purge.cron:0 0 4 * * *}")
    public void scheduledPurge() {
        purgeRepository();
    }

    public Future<Long> purgeRepository() {
        logger.info("Purging secure file collections…");
        final Long count = secureFileCollectionRepository.removeByPurgeInstantBefore(Instant.now(clock));

        logger.debug("Purged files - " + count + " secure file collections purged.");

        logger.info("Finished purging secure file collections.");
        return new AsyncResult<>(count);
    }
}
