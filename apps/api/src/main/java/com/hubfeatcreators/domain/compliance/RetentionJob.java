package com.hubfeatcreators.domain.compliance;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RetentionJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);

    private final JdbcTemplate jdbc;
    private final RetentionRunRepository runRepo;

    public RetentionJob(JdbcTemplate jdbc, RetentionRunRepository runRepo) {
        this.jdbc = jdbc;
        this.runRepo = runRepo;
    }

    /** Runs daily at 03:00 UTC. ADR-011 retention policies. */
    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @SchedulerLock(name = "lgpd-retencao-diaria", lockAtMostFor = "30m")
    @Transactional
    public void executar() {
        log.info("retention.job.inicio");
        LocalDate hoje = LocalDate.now(ZoneOffset.UTC);

        // influenciadores: anon after 180d soft-deleted
        int infAnon = anonymizarInfluenciadores(hoje);
        int marcasAnon = anonymizarMarcas(hoje);
        int contatosAnon = anonymizarContatos(hoje);
        int jobsPurgados = purgarJobsAntigos(hoje);
        int auditPurgados = purgarAuditLogAntigo(hoje);

        List.of(
                new RetentionRun(hoje, "influenciadores", infAnon, 0, 0),
                new RetentionRun(hoje, "marcas", marcasAnon, 0, 0),
                new RetentionRun(hoje, "contatos", contatosAnon, 0, 0),
                new RetentionRun(hoje, "job", 0, jobsPurgados, 0),
                new RetentionRun(hoje, "audit_log", 0, auditPurgados, 0)
        ).forEach(runRepo::save);

        log.info("retention.job.fim infAnon={} marcasAnon={} contatosAnon={} jobsPurgados={} auditPurgados={}",
                infAnon, marcasAnon, contatosAnon, jobsPurgados, auditPurgados);
    }

    private int anonymizarInfluenciadores(LocalDate hoje) {
        Instant cutoff = hoje.minus(180, ChronoUnit.DAYS).atStartOfDay(ZoneOffset.UTC).toInstant();
        return jdbc.update("""
                UPDATE influenciadores
                SET nome = 'Titular #' || substring(id::text, 1, 8),
                    handles = '{}',
                    observacoes = NULL,
                    updated_at = now()
                WHERE deleted_at IS NOT NULL
                  AND deleted_at < ?
                  AND nome NOT LIKE 'Titular #%'
                """, cutoff);
    }

    private int anonymizarMarcas(LocalDate hoje) {
        Instant cutoff = hoje.minus(180, ChronoUnit.DAYS).atStartOfDay(ZoneOffset.UTC).toInstant();
        return jdbc.update("""
                UPDATE marcas
                SET nome = 'Marca #' || substring(id::text, 1, 8),
                    site = NULL,
                    observacoes = NULL,
                    updated_at = now()
                WHERE deleted_at IS NOT NULL
                  AND deleted_at < ?
                  AND nome NOT LIKE 'Marca #%'
                """, cutoff);
    }

    private int anonymizarContatos(LocalDate hoje) {
        Instant cutoff = hoje.minus(180, ChronoUnit.DAYS).atStartOfDay(ZoneOffset.UTC).toInstant();
        return jdbc.update("""
                UPDATE contatos
                SET nome = 'Titular #' || substring(id::text, 1, 8),
                    email = NULL,
                    telefone = NULL,
                    updated_at = now()
                WHERE deleted_at IS NOT NULL
                  AND deleted_at < ?
                  AND nome NOT LIKE 'Titular #%'
                """, cutoff);
    }

    private int purgarJobsAntigos(LocalDate hoje) {
        Instant cutoff = hoje.minus(7, ChronoUnit.DAYS).atStartOfDay(ZoneOffset.UTC).toInstant();
        return jdbc.update("""
                DELETE FROM job
                WHERE status IN ('OK','MORTO')
                  AND updated_at < ?
                """, cutoff);
    }

    private int purgarAuditLogAntigo(LocalDate hoje) {
        Instant cutoff = hoje.minus(180, ChronoUnit.DAYS).atStartOfDay(ZoneOffset.UTC).toInstant();
        // Pseudonymize older entries rather than delete (preserve hash chain)
        return jdbc.update("""
                UPDATE audit_log
                SET usuario_id = NULL,
                    payload = '{}'::jsonb
                WHERE created_at < ?
                  AND usuario_id IS NOT NULL
                """, cutoff);
    }
}
