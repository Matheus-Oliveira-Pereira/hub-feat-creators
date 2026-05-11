package com.hubfeatcreators.domain.relatorio;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RefreshMvScheduler {

    private static final Logger log = LoggerFactory.getLogger(RefreshMvScheduler.class);

    private final RelatorioService service;

    public RefreshMvScheduler(RelatorioService service) {
        this.service = service;
    }

    /** Daily at 03:00 BRT (06:00 UTC). */
    @Scheduled(cron = "0 0 6 * * *", zone = "UTC")
    @SchedulerLock(name = "relatorio-refresh-mvs", lockAtMostFor = "10m")
    public void refresh() {
        log.info("relatorio.mv.refresh.inicio");
        service.refreshMvs();
        log.info("relatorio.mv.refresh.fim");
    }
}
