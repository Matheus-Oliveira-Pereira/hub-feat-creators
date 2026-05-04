package com.hubfeatcreators.infra.job;

import java.net.InetAddress;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JobWorker {

    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);
    private static final int BATCH_SIZE = 10;

    // Backoff: 1m, 5m, 30m, 2h
    private static final long[] BACKOFF_MINUTES = {1, 5, 30, 120};

    private final JobRepository repo;
    private final Map<String, JobHandler> handlers;

    public JobWorker(JobRepository repo, Map<String, JobHandler> handlers) {
        this.repo = repo;
        this.handlers = handlers;
    }

    @Scheduled(fixedDelay = 5000)
    @SchedulerLock(name = "job_worker", lockAtLeastFor = "4s", lockAtMostFor = "30s")
    @Transactional
    public void poll() {
        List<Job> jobs = repo.pickupPending(Instant.now(), BATCH_SIZE);
        if (jobs.isEmpty()) return;

        String workerId = resolveWorkerId();

        for (Job job : jobs) {
            job.setStatus(JobStatus.PROCESSANDO);
            job.setIniciadoEm(Instant.now());
            job.setWorkerId(workerId);
            job.setTentativas(job.getTentativas() + 1);
            repo.save(job);

            try {
                JobHandler handler = handlers.get(job.getTipo());
                if (handler == null) {
                    fail(job, "Handler não encontrado: " + job.getTipo());
                    continue;
                }
                handler.handle(job);
                job.setStatus(JobStatus.OK);
                job.setConcluidoEm(Instant.now());
                repo.save(job);
                log.info("job.ok tipo={} id={}", job.getTipo(), job.getId());
            } catch (Exception e) {
                log.warn(
                        "job.fail tipo={} id={} tentativa={}",
                        job.getTipo(),
                        job.getId(),
                        job.getTentativas(),
                        e);
                if (job.getTentativas() >= job.getMaxTentativas()) {
                    fail(job, e.getMessage());
                } else {
                    long backoff =
                            BACKOFF_MINUTES[
                                    Math.min(job.getTentativas() - 1, BACKOFF_MINUTES.length - 1)];
                    job.setStatus(JobStatus.PENDENTE);
                    job.setProximaTentativaEm(Instant.now().plus(backoff, ChronoUnit.MINUTES));
                    job.setUltimoErro(e.getMessage());
                    repo.save(job);
                }
            }
        }
    }

    private void fail(Job job, String erro) {
        job.setStatus(JobStatus.MORTO);
        job.setConcluidoEm(Instant.now());
        job.setUltimoErro(erro);
        repo.save(job);
        log.error("job.dead tipo={} id={} erro={}", job.getTipo(), job.getId(), erro);
    }

    private String resolveWorkerId() {
        try {
            return InetAddress.getLocalHost().getHostName() + ":" + ProcessHandle.current().pid();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
