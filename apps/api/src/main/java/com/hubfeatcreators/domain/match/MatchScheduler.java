package com.hubfeatcreators.domain.match;

import com.hubfeatcreators.infra.job.JobService;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(MatchScheduler.class);

    private final CreatorProfileFeatureRepository featureRepo;
    private final JobService jobService;

    public MatchScheduler(CreatorProfileFeatureRepository featureRepo, JobService jobService) {
        this.featureRepo = featureRepo;
        this.jobService = jobService;
    }

    private static final int CHUNK = 200;

    @Scheduled(cron = "0 30 3 * * *", zone = "UTC")
    @SchedulerLock(name = "match_daily_feature_sync", lockAtMostFor = "PT2H")
    public void syncCreatorFeatures() {
        String date = LocalDate.now().toString();
        int page = 0;
        long total = 0;
        Page<CreatorProfileFeature> chunk;
        do {
            chunk = featureRepo.findAll(PageRequest.of(page++, CHUNK));
            for (CreatorProfileFeature f : chunk) {
                String key = "REEMBED:CREATOR:" + f.getInfluenciadorId() + ":" + date;
                jobService.enqueue(
                        "REEMBED",
                        Map.of("type", "CREATOR", "id", f.getInfluenciadorId().toString()),
                        UUID.nameUUIDFromBytes(key.getBytes()));
            }
            total += chunk.getNumberOfElements();
        } while (chunk.hasNext());
        log.info("match.scheduler.enqueued total={}", total);
    }
}
