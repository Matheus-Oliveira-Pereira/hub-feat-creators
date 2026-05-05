package com.hubfeatcreators.notificacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubfeatcreators.config.AppProperties;
import com.hubfeatcreators.domain.notificacao.WebPushSender;
import com.hubfeatcreators.domain.notificacao.WebpushSubscription;
import com.hubfeatcreators.domain.notificacao.WebpushSubscriptionRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.UUID;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebPushSenderTest {

    @Mock WebpushSubscriptionRepository subRepo;
    @Mock PushService pushService;
    @Mock HttpResponse httpResponse;
    @Mock StatusLine statusLine;

    WebPushSender sender;

    @BeforeEach
    void setUp() {
        sender =
                new WebPushSender(
                        new AppProperties(), subRepo, new SimpleMeterRegistry(), new ObjectMapper());
        try {
            var field = WebPushSender.class.getDeclaredField("pushService");
            field.setAccessible(true);
            field.set(sender, pushService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    WebpushSubscription makeSub(String endpoint) {
        return new WebpushSubscription(UUID.randomUUID(), endpoint, "p256dh", "auth", "TestAgent");
    }

    @Test
    void send_statusCode_410_marca_subscription_inativa() throws Exception {
        var sub = makeSub("https://push.example.com/sub1");
        when(subRepo.findByUsuarioIdAndAtivaTrue(any())).thenReturn(List.of(sub));
        when(statusLine.getStatusCode()).thenReturn(410);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);

        try (MockedConstruction<Notification> mocked =
                mockConstruction(
                        Notification.class,
                        (notification, ctx) ->
                                when(pushService.send(notification)).thenReturn(httpResponse))) {

            sender.send(sub.getUsuarioId(), "Titulo", "Mensagem", "/");
        }

        assertThat(sub.isAtiva()).isFalse();
        verify(subRepo).save(sub);
    }

    @Test
    void send_statusCode_200_atualiza_lastUsedAt() throws Exception {
        var sub = makeSub("https://push.example.com/sub2");
        when(subRepo.findByUsuarioIdAndAtivaTrue(any())).thenReturn(List.of(sub));
        when(statusLine.getStatusCode()).thenReturn(201);
        when(httpResponse.getStatusLine()).thenReturn(statusLine);

        try (MockedConstruction<Notification> mocked =
                mockConstruction(
                        Notification.class,
                        (notification, ctx) ->
                                when(pushService.send(notification)).thenReturn(httpResponse))) {

            sender.send(sub.getUsuarioId(), "Titulo", "Mensagem", "/prospeccao?id=abc");
        }

        assertThat(sub.getLastUsedAt()).isNotNull();
        verify(subRepo).save(sub);
    }

    @Test
    void send_pushService_null_nao_lanca_excecao() throws Exception {
        var field = WebPushSender.class.getDeclaredField("pushService");
        field.setAccessible(true);
        field.set(sender, null);

        sender.send(UUID.randomUUID(), "T", "M", "/");
        verify(subRepo, never()).findByUsuarioIdAndAtivaTrue(any());
    }
}
