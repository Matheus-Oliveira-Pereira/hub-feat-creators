package com.hubfeatcreators.domain.notificacao;

import com.hubfeatcreators.infra.job.Job;
import com.hubfeatcreators.infra.job.JobHandler;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processa NOTIFICACAO_DIGEST: verifica se há não-lidas no sistema e loga resumo. Envio real por
 * e-mail será integrado quando PRD-004 expuser API de envio interno.
 */
@Component("NOTIFICACAO_DIGEST")
public class NotificacaoDigestJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoDigestJobHandler.class);

    private final NotificacaoRepository notificacaoRepo;
    private final MeterRegistry meterRegistry;

    public NotificacaoDigestJobHandler(
            NotificacaoRepository notificacaoRepo, MeterRegistry meterRegistry) {
        this.notificacaoRepo = notificacaoRepo;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional(readOnly = true)
    public void handle(Job job) {
        Map<String, Object> payload = job.getPayload();
        String data = (String) payload.get("data");

        long naoLidas = notificacaoRepo.count();

        if (naoLidas == 0) {
            log.info("notificacao.digest.skip data={} motivo=sem_nao_lidas", data);
            return;
        }

        log.info("notificacao.digest.resumo data={} total={}", data, naoLidas);
        Counter.builder("notificacao_digest_processado_total").register(meterRegistry).increment();
    }
}
