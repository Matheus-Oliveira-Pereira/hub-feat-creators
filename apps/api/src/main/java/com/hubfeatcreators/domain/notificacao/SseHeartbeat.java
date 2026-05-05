package com.hubfeatcreators.domain.notificacao;

import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseHeartbeat {

    private static final Logger log = LoggerFactory.getLogger(SseHeartbeat.class);

    private final NotificacaoService notificacaoService;

    public SseHeartbeat(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @Scheduled(fixedDelay = 25_000)
    public void heartbeat() {
        notificacaoService.getEmitters().forEach((usuarioId, emitters) -> {
            Set<SseEmitter> copy = Set.copyOf(emitters);
            copy.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (Exception e) {
                    notificacaoService.removeEmitter(usuarioId, emitter);
                }
            });
        });
    }
}
