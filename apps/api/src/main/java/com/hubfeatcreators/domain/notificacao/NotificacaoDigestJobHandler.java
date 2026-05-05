package com.hubfeatcreators.domain.notificacao;

import com.hubfeatcreators.infra.job.Job;
import com.hubfeatcreators.infra.job.JobHandler;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Processa NOTIFICACAO_DIGEST: verifica se há não-lidas na assessoria e loga resumo.
 * Envio real por e-mail será integrado quando PRD-004 expuser API de envio interno.
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
        UUID assessoriaId = UUID.fromString((String) payload.get("assessoriaId"));
        String data = (String) payload.get("data");

        long naoLidas = notificacaoRepo.countNaoLidasByAssessoriaId(assessoriaId);

        if (naoLidas == 0) {
            log.info("notificacao.digest.skip assessoriaId={} data={} motivo=sem_nao_lidas", assessoriaId, data);
            return;
        }

        log.info("notificacao.digest.resumo assessoriaId={} data={} naoLidas={}", assessoriaId, data, naoLidas);
        Counter.builder("notificacao_digest_processado_total").register(meterRegistry).increment();
    }
}
