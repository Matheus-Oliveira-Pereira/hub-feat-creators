package com.hubfeatcreators.domain.tarefa;

import com.hubfeatcreators.domain.assessoria.AssessoriaRepository;
import com.hubfeatcreators.infra.job.JobService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DigestScheduler {

    private static final Logger log = LoggerFactory.getLogger(DigestScheduler.class);
    private static final ZoneId TZ_BR = ZoneId.of("America/Sao_Paulo");
    private static final int CHUNK = 200;

    private final AssessoriaRepository assessoriaRepo;
    private final JobService jobService;
    private final MeterRegistry meterRegistry;

    public DigestScheduler(
            AssessoriaRepository assessoriaRepo,
            JobService jobService,
            MeterRegistry meterRegistry) {
        this.assessoriaRepo = assessoriaRepo;
        this.jobService = jobService;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Sao_Paulo")
    @SchedulerLock(name = "email_digest_diario", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void enfileirarDigestDiario() {
        String dataHoje = LocalDate.now(TZ_BR).toString();
        log.info("digest.scheduler.start data={}", dataHoje);

        int page = 0;
        Page<com.hubfeatcreators.domain.assessoria.Assessoria> chunk;
        do {
            chunk = assessoriaRepo.findAll(PageRequest.of(page++, CHUNK));
            chunk.forEach(
                    assessoria -> {
                        try {
                            var idempotencyKey =
                                    java.util.UUID.nameUUIDFromBytes(
                                            (assessoria.getId().toString()
                                                            + ":EMAIL_DIGEST:"
                                                            + dataHoje)
                                                    .getBytes());
                            jobService.enqueue(
                                    assessoria.getId(),
                                    "EMAIL_DIGEST",
                                    Map.of(
                                            "assessoriaId",
                                            assessoria.getId().toString(),
                                            "data",
                                            dataHoje),
                                    idempotencyKey);
                            Counter.builder("digest_enfileirado_total")
                                    .register(meterRegistry)
                                    .increment();
                        } catch (Exception e) {
                            log.error(
                                    "digest.scheduler.error assessoriaId={} msg={}",
                                    assessoria.getId(),
                                    e.getMessage(),
                                    e);
                            Counter.builder("digest_falha_total")
                                    .register(meterRegistry)
                                    .increment();
                        }
                    });
        } while (chunk.hasNext());

        log.info("digest.scheduler.done data={}", dataHoje);
    }
}
