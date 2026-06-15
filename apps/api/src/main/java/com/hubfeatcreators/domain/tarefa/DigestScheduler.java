package com.hubfeatcreators.domain.tarefa;

import com.hubfeatcreators.infra.job.JobService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DigestScheduler {

    private static final Logger log = LoggerFactory.getLogger(DigestScheduler.class);
    private static final ZoneId TZ_BR = ZoneId.of("America/Sao_Paulo");

    private final JobService jobService;
    private final MeterRegistry meterRegistry;

    public DigestScheduler(JobService jobService, MeterRegistry meterRegistry) {
        this.jobService = jobService;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Sao_Paulo")
    @SchedulerLock(name = "email_digest_diario", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void enfileirarDigestDiario() {
        String dataHoje = LocalDate.now(TZ_BR).toString();
        log.info("digest.scheduler.start data={}", dataHoje);

        try {
            var idempotencyKey =
                    java.util.UUID.nameUUIDFromBytes(
                            ("EMAIL_DIGEST:" + dataHoje).getBytes());
            jobService.enqueue(
                    "EMAIL_DIGEST",
                    Map.of("data", dataHoje),
                    idempotencyKey);
            Counter.builder("digest_enfileirado_total").register(meterRegistry).increment();
        } catch (Exception e) {
            log.error("digest.scheduler.error msg={}", e.getMessage(), e);
            Counter.builder("digest_falha_total").register(meterRegistry).increment();
        }

        log.info("digest.scheduler.done data={}", dataHoje);
    }
}
