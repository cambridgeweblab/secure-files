package ucles.weblab.common.files.domain;

import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.Future;

/**
 * DDD service interface, for methods which do not naturally form part of the {@link SecureFileCollectionRepository}.
 *
 * @since 24/07/15
 */
public interface SecureFileCollectionService {
    @Scheduled(cron = "${files.purge.cron:0 0 4 * * *}")
    void scheduledPurge();

    /**
     * Purge any file collections with purge instants in the past.
     *
     * @return a future to obtain the number of collections purged, or any exception which occurred during the purge
     */
    Future<Long> purgeRepository();
}
