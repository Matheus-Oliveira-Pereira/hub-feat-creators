package com.hubfeatcreators.domain.social;

import com.hubfeatcreators.infra.job.JobService;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SocialTokenRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(SocialTokenRefreshScheduler.class);

    private final SocialAccountRepository accountRepo;
    private final JobService jobService;

    public SocialTokenRefreshScheduler(SocialAccountRepository accountRepo, JobService jobService) {
        this.accountRepo = accountRepo;
        this.jobService = jobService;
    }

    // every 6h
    @Scheduled(cron = "0 0 */6 * * *", zone = "UTC")
    public void scheduleRefresh() {
        Instant threshold = Instant.now().plusSeconds(86400); // expiring within 24h
        var accounts = accountRepo.findDueForRefresh(threshold);
        log.info("SocialTokenRefreshScheduler: enqueuing {} refresh jobs", accounts.size());
        for (SocialAccount account : accounts) {
            jobService.enqueue(
                    account.getAssessoriaId(),
                    "SOCIAL_REFRESH_TOKEN",
                    Map.of("accountId", account.getId().toString()),
                    UUID.nameUUIDFromBytes(
                            ("SOCIAL_REFRESH:"
                                            + account.getId()
                                            + ":"
                                            + Instant.now().getEpochSecond() / 21600)
                                    .getBytes()));
        }
    }
}
