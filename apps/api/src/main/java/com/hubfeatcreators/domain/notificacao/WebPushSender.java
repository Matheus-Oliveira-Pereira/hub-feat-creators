package com.hubfeatcreators.domain.notificacao;

import com.hubfeatcreators.config.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebPushSender {

    private static final Logger log = LoggerFactory.getLogger(WebPushSender.class);

    private final AppProperties appProperties;
    private final WebpushSubscriptionRepository subRepo;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    private PushService pushService;

    public WebPushSender(
            AppProperties appProperties,
            WebpushSubscriptionRepository subRepo,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.subRepo = subRepo;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        var wp = appProperties.getWebpush();
        if (wp.getPublicKey().isBlank() || wp.getPrivateKey().isBlank()) {
            log.warn("webpush.init.skip motivo=keys_not_configured");
            return;
        }
        try {
            pushService = new PushService(wp.getPublicKey(), wp.getPrivateKey(), wp.getSubject());
            log.info("webpush.init.ok");
        } catch (Exception e) {
            log.error("webpush.init.error msg={}", e.getMessage(), e);
        }
    }

    @Transactional
    public void send(UUID usuarioId, String titulo, String mensagem, String targetUrl) {
        if (pushService == null) {
            log.debug("webpush.send.skip usuarioId={} motivo=push_service_not_initialized", usuarioId);
            return;
        }

        byte[] payloadBytes;
        try {
            payloadBytes = objectMapper.writeValueAsBytes(Map.of(
                    "titulo", titulo,
                    "mensagem", mensagem,
                    "url", targetUrl != null ? targetUrl : "/"));
        } catch (Exception e) {
            log.error("webpush.payload.error msg={}", e.getMessage());
            return;
        }

        subRepo.findByUsuarioIdAndAtivaTrue(usuarioId).forEach(sub -> {
            try {
                Notification notification = new Notification(
                        sub.getEndpoint(), sub.getP256dh(), sub.getAuthSecret(), payloadBytes);
                HttpResponse response = pushService.send(notification);
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 410 || statusCode == 404) {
                    sub.setAtiva(false);
                    subRepo.save(sub);
                    log.info("webpush.subscription.expired usuarioId={} endpoint={}", usuarioId, sub.getEndpoint());
                    meterRegistry.counter("webpush_enviado_total", "status", "expired").increment();
                } else {
                    sub.setLastUsedAt(Instant.now());
                    subRepo.save(sub);
                    meterRegistry.counter("webpush_enviado_total", "status", "ok").increment();
                    log.debug("webpush.send.ok usuarioId={} status={}", usuarioId, statusCode);
                }
            } catch (Exception e) {
                log.error("webpush.send.error usuarioId={} endpoint={} msg={}", usuarioId, sub.getEndpoint(), e.getMessage(), e);
                meterRegistry.counter("webpush_enviado_total", "status", "erro").increment();
            }
        });
    }
}
