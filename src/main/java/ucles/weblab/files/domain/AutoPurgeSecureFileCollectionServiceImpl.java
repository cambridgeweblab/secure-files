package ucles.weblab.files.domain;

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

    private final SecureFileCollectionRepository secureFileCollectionRepository;
    private final SecureFileRepository secureFileRepository;

    public AutoPurgeSecureFileCollectionServiceImpl(SecureFileCollectionRepository secureFileCollectionRepository, SecureFileRepository secureFileRepository) {
        this.secureFileCollectionRepository = secureFileCollectionRepository;
        this.secureFileRepository = secureFileRepository;
    }

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
        Instant now = Instant.now(clock);
        final Integer fileCount = secureFileRepository.deleteByCollectionPurgeInstantBefore(now);
        final Long count = secureFileCollectionRepository.removeByPurgeInstantBefore(now);

        logger.debug("Purged files - {} secure file collections purged containing {} files.", count, fileCount);

        logger.info("Finished purging secure file collections.");
        return new AsyncResult<>(count);
    }
}
