package com.hubfeatcreators.infra.push;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubfeatcreators.domain.notificacao.DeviceSubscription;
import com.hubfeatcreators.domain.notificacao.DeviceSubscriptionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExpoPushSender {

    private static final Logger log = LoggerFactory.getLogger(ExpoPushSender.class);
    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final DeviceSubscriptionRepository deviceRepo;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final HttpClient httpClient;

    public ExpoPushSender(
            DeviceSubscriptionRepository deviceRepo,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.deviceRepo = deviceRepo;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Transactional
    public void send(UUID userId, String titulo, String mensagem, String targetUrl) {
        List<DeviceSubscription> subs = deviceRepo.findByUserIdAndAtivaTrue(userId);
        if (subs.isEmpty()) return;

        for (DeviceSubscription sub : subs) {
            sendToDevice(sub, titulo, mensagem, targetUrl);
        }
    }

    private void sendToDevice(
            DeviceSubscription sub, String titulo, String mensagem, String targetUrl) {
        try {
            Map<String, Object> message =
                    Map.of(
                            "to",
                            sub.getToken(),
                            "title",
                            titulo,
                            "body",
                            mensagem,
                            "data",
                            Map.of("url", targetUrl != null ? targetUrl : "/"),
                            "sound",
                            "default");

            String body = objectMapper.writeValueAsString(message);
            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(URI.create(EXPO_PUSH_URL))
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                sub.setUltimoUso(Instant.now());
                deviceRepo.save(sub);
                meterRegistry.counter("expo_push_enviado_total", "status", "ok").increment();
                log.debug("expo_push.ok userId={} canal={}", sub.getUserId(), sub.getCanal());
            } else {
                meterRegistry.counter("expo_push_enviado_total", "status", "erro").increment();
                log.warn(
                        "expo_push.error userId={} status={} body={}",
                        sub.getUserId(),
                        response.statusCode(),
                        response.body());
            }
        } catch (Exception e) {
            meterRegistry.counter("expo_push_enviado_total", "status", "erro").increment();
            log.error("expo_push.exception userId={} msg={}", sub.getUserId(), e.getMessage(), e);
        }
    }
}
