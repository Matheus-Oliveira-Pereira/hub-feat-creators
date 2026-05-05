package com.hubfeatcreators.domain.notificacao;

import com.hubfeatcreators.infra.web.BusinessException;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class NotificacaoService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);
    private static final int THROTTLE_MINUTOS = 5;
    private static final int MAX_PAGE = 50;

    private final NotificacaoRepository repo;
    private final NotificacaoPreferenciaRepository prefRepo;
    private final WebpushSubscriptionRepository subRepo;
    private final NotificacaoDedupeRepository dedupeRepo;
    private final WebPushSender webPushSender;
    private final MeterRegistry meterRegistry;

    private final Map<UUID, Set<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public NotificacaoService(
            NotificacaoRepository repo,
            NotificacaoPreferenciaRepository prefRepo,
            WebpushSubscriptionRepository subRepo,
            NotificacaoDedupeRepository dedupeRepo,
            WebPushSender webPushSender,
            MeterRegistry meterRegistry) {
        this.repo = repo;
        this.prefRepo = prefRepo;
        this.subRepo = subRepo;
        this.dedupeRepo = dedupeRepo;
        this.webPushSender = webPushSender;
        this.meterRegistry = meterRegistry;
        Gauge.builder("sse_conexoes_ativas", emitters, m -> m.values().stream().mapToInt(Set::size).sum())
                .register(meterRegistry);
    }

    @Transactional
    public Notificacao criar(
            UUID assessoriaId,
            UUID usuarioId,
            NotificacaoTipo tipo,
            NotificacaoPrioridade prioridade,
            String titulo,
            String mensagem,
            Map<String, Object> payload,
            String alvoTipo,
            UUID alvoId) {

        if (usuarioId == null) {
            log.debug("notificacao.skip tipo={} motivo=usuario_id_null (owner lookup pendente)", tipo);
            return null;
        }
        boolean inappHabilitado = isHabilitado(usuarioId, tipo, NotificacaoCanal.INAPP, true);
        if (!inappHabilitado) {
            log.debug("notificacao.skip usuarioId={} tipo={} canal=INAPP motivo=desabilitado", usuarioId, tipo);
            return null;
        }

        // Throttle / dedupe
        String dedupeKey = tipo.name() + ":" + (alvoId != null ? alvoId : usuarioId);
        Optional<NotificacaoDedupe> dedupeOpt = dedupeRepo.findById(dedupeKey);
        if (dedupeOpt.isPresent()) {
            Instant threshold = Instant.now().minusSeconds(THROTTLE_MINUTOS * 60L);
            if (dedupeOpt.get().getLastEmitted().isAfter(threshold)) {
                // Agrupar: incrementa agrupadas na última notificação não-lida do mesmo tipo+alvo
                Page<Notificacao> pending = repo.findPendingForDedupe(
                        assessoriaId, usuarioId, tipo, alvoId, PageRequest.of(0, 1));
                if (!pending.isEmpty()) {
                    Notificacao existente = pending.getContent().get(0);
                    existente.setAgrupadas(existente.getAgrupadas() + 1);
                    repo.save(existente);
                    dedupeOpt.get().setLastEmitted(Instant.now());
                    dedupeRepo.save(dedupeOpt.get());
                    log.debug("notificacao.agrupada id={} agrupadas={}", existente.getId(), existente.getAgrupadas());
                    return existente;
                }
            }
        }

        Notificacao n = new Notificacao(assessoriaId, usuarioId, tipo, prioridade, titulo, mensagem, payload, alvoTipo, alvoId);
        repo.save(n);

        // Atualizar dedupe
        NotificacaoDedupe dedupe = dedupeOpt.orElse(new NotificacaoDedupe(dedupeKey, Instant.now()));
        dedupe.setLastEmitted(Instant.now());
        dedupeRepo.save(dedupe);

        meterRegistry.counter("notificacao_criada_total", "tipo", tipo.name()).increment();
        log.info("notificacao.criada id={} tipo={} usuarioId={}", n.getId(), tipo, usuarioId);

        // SSE
        notificarEmitters(usuarioId);

        // Web Push
        if (isHabilitado(usuarioId, tipo, NotificacaoCanal.PUSH, false)) {
            webPushSender.send(usuarioId, titulo, mensagem, buildAlvoUrl(alvoTipo, alvoId));
        }

        return n;
    }

    @Transactional(readOnly = true)
    public Page<Notificacao> listar(UUID assessoriaId, UUID usuarioId, NotificacaoTipo tipo, boolean apenasNaoLidas, int page, int size) {
        var pageable = PageRequest.of(Math.max(0, page), Math.min(size, MAX_PAGE));
        return repo.findFiltered(assessoriaId, usuarioId, tipo, apenasNaoLidas, pageable);
    }

    @Transactional(readOnly = true)
    public long contarNaoLidas(UUID assessoriaId, UUID usuarioId) {
        return repo.countByAssessoriaIdAndUsuarioIdAndLidaEmIsNull(assessoriaId, usuarioId);
    }

    @Transactional
    public void marcarLida(UUID assessoriaId, UUID notificacaoId, UUID usuarioId) {
        Notificacao n = repo.findByIdAndAssessoriaIdAndUsuarioId(notificacaoId, assessoriaId, usuarioId)
                .orElseThrow(() -> BusinessException.notFound("NOTIFICACAO"));
        if (n.getLidaEm() == null) {
            n.setLidaEm(Instant.now());
            repo.save(n);
            notificarEmitters(usuarioId);
        }
    }

    @Transactional
    public void marcarTodasLidas(UUID assessoriaId, UUID usuarioId) {
        int count = repo.marcarTodasLidas(assessoriaId, usuarioId, Instant.now());
        if (count > 0) {
            notificarEmitters(usuarioId);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificacaoPreferencia> preferencias(UUID usuarioId) {
        List<NotificacaoPreferencia> saved = prefRepo.findByUsuarioId(usuarioId);
        Map<NotificacaoTipo, Map<NotificacaoCanal, Boolean>> idx = new EnumMap<>(NotificacaoTipo.class);
        for (NotificacaoPreferencia p : saved) {
            idx.computeIfAbsent(p.getTipo(), k -> new EnumMap<>(NotificacaoCanal.class))
               .put(p.getCanal(), p.isHabilitado());
        }
        List<NotificacaoPreferencia> result = new ArrayList<>(saved);
        for (NotificacaoTipo tipo : NotificacaoTipo.values()) {
            for (NotificacaoCanal canal : NotificacaoCanal.values()) {
                if (!idx.containsKey(tipo) || !idx.get(tipo).containsKey(canal)) {
                    boolean defaultValue = canal == NotificacaoCanal.INAPP;
                    result.add(new NotificacaoPreferencia(usuarioId, tipo, canal, defaultValue));
                }
            }
        }
        return result;
    }

    @Transactional
    public NotificacaoPreferencia atualizarPreferencia(UUID usuarioId, NotificacaoTipo tipo, NotificacaoCanal canal, boolean habilitado) {
        NotificacaoPreferencia pref = prefRepo.findByUsuarioIdAndTipoAndCanal(usuarioId, tipo, canal)
                .orElse(new NotificacaoPreferencia(usuarioId, tipo, canal, habilitado));
        pref.setHabilitado(habilitado);
        return prefRepo.save(pref);
    }

    // SSE emitter management

    public SseEmitter createEmitter(UUID usuarioId) {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.computeIfAbsent(usuarioId, k -> new CopyOnWriteArraySet<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(usuarioId, emitter));
        emitter.onTimeout(() -> removeEmitter(usuarioId, emitter));
        emitter.onError(e -> removeEmitter(usuarioId, emitter));
        return emitter;
    }

    public void removeEmitter(UUID usuarioId, SseEmitter emitter) {
        Set<SseEmitter> set = emitters.get(usuarioId);
        if (set != null) {
            set.remove(emitter);
            if (set.isEmpty()) emitters.remove(usuarioId);
        }
    }

    public Map<UUID, Set<SseEmitter>> getEmitters() {
        return Collections.unmodifiableMap(emitters);
    }

    public void notificarEmitters(UUID usuarioId) {
        Set<SseEmitter> set = emitters.get(usuarioId);
        if (set == null || set.isEmpty()) return;
        set.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("notificacao").data("update"));
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
    }

    // Helpers

    private boolean isHabilitado(UUID usuarioId, NotificacaoTipo tipo, NotificacaoCanal canal, boolean defaultValue) {
        return prefRepo.findByUsuarioIdAndTipoAndCanal(usuarioId, tipo, canal)
                .map(NotificacaoPreferencia::isHabilitado)
                .orElse(defaultValue);
    }

    private String buildAlvoUrl(String alvoTipo, UUID alvoId) {
        if (alvoTipo == null || alvoId == null) return "/";
        return switch (alvoTipo) {
            case "TAREFA" -> "/tarefas?id=" + alvoId;
            case "PROSPECCAO" -> "/prospeccao?id=" + alvoId;
            case "EMAIL_ACCOUNT" -> "/email";
            default -> "/";
        };
    }
}
