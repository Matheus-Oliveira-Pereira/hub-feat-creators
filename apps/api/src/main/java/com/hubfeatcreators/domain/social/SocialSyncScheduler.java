package com.hubfeatcreators.domain.social;

import com.hubfeatcreators.infra.job.JobService;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SocialSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SocialSyncScheduler.class);

    private final SocialAccountRepository accountRepo;
    private final JobService jobService;

    public SocialSyncScheduler(SocialAccountRepository accountRepo, JobService jobService) {
        this.accountRepo = accountRepo;
        this.jobService = jobService;
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    @SchedulerLock(name = "social_sync_daily", lockAtMostFor = "PT1H", lockAtLeastFor = "PT1M")
    public void scheduleDaily() {
        var accounts = accountRepo.findAllAtiva();
        log.info("SocialSyncScheduler: enqueuing {} accounts", accounts.size());
        for (SocialAccount account : accounts) {
            jobService.enqueue(
                    "SOCIAL_SYNC",
                    Map.of("accountId", account.getId().toString()),
                    UUID.nameUUIDFromBytes(
                            ("SOCIAL_SYNC:" + account.getId() + ":" + LocalDate.now()).getBytes()));
        }
    }
}
