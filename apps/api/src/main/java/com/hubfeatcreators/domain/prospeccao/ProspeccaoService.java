package com.hubfeatcreators.domain.prospeccao;

import com.hubfeatcreators.domain.notificacao.events.ProspeccaoMudouStatusEvent;
import com.hubfeatcreators.infra.audit.AuditLog;
import com.hubfeatcreators.infra.audit.AuditLogService;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.web.BusinessException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProspeccaoService {

    private static final int MAX_PAGE = 100;

    private final ProspeccaoRepository repo;
    private final ProspeccaoEventoRepository eventoRepo;
    private final MeterRegistry meterRegistry;
    private final AuditLogService auditLog;
    private final ApplicationEventPublisher eventPublisher;

    public ProspeccaoService(
            ProspeccaoRepository repo,
            ProspeccaoEventoRepository eventoRepo,
            MeterRegistry meterRegistry,
            AuditLogService auditLog,
            ApplicationEventPublisher eventPublisher) {
        this.repo = repo;
        this.eventoRepo = eventoRepo;
        this.meterRegistry = meterRegistry;
        this.auditLog = auditLog;
        this.eventPublisher = eventPublisher;
    }

    // ─── Read ───────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<Prospeccao> listar(
            AuthPrincipal principal,
            ProspeccaoStatus status,
            UUID assessorId,
            UUID marcaId,
            String nome,
            int page,
            int size) {
        var pageable = PageRequest.of(Math.max(0, page), Math.min(size, MAX_PAGE));
        if (canSeeAll(principal)) {
            return repo.findAllOwner(
                    principal.assessoriaId(), status, assessorId, marcaId, nome, pageable);
        }
        return repo.findAllAssessor(
                principal.assessoriaId(),
                principal.usuarioId(),
                status,
                assessorId,
                marcaId,
                nome,
                pageable);
    }

    @Transactional(readOnly = true)
    public Prospeccao buscar(AuthPrincipal principal, UUID id) {
        Prospeccao p =
                repo.findById(id).orElseThrow(() -> BusinessException.notFound("PROSPECCAO"));
        ensureVisible(principal, p);
        return p;
    }

    // ─── Write ──────────────────────────────────────────────────────────────
    @Transactional
    public Prospeccao criar(
            AuthPrincipal principal,
            UUID marcaId,
            UUID influenciadorId,
            UUID assessorResponsavelId,
            String titulo,
            Long valorEstimadoCentavos,
            String proximaAcao,
            LocalDate proximaAcaoEm,
            String observacoes,
            String[] tags) {
        Prospeccao p =
                new Prospeccao(
                        principal.assessoriaId(),
                        marcaId,
                        assessorResponsavelId != null
                                ? assessorResponsavelId
                                : principal.usuarioId(),
                        titulo,
                        principal.usuarioId());
        p.setInfluenciadorId(influenciadorId);
        p.setValorEstimadoCentavos(valorEstimadoCentavos);
        p.setProximaAcao(proximaAcao);
        p.setProximaAcaoEm(proximaAcaoEm);
        p.setObservacoes(observacoes);
        if (tags != null) p.setTags(tags);

        Prospeccao salvo = repo.save(p);

        Counter.builder("prospeccoes_criadas_total").register(meterRegistry).increment();

        eventoRepo.save(
                new ProspeccaoEvento(
                        salvo.getId(),
                        principal.assessoriaId(),
                        EventoTipo.STATUS_CHANGE,
                        Map.of("para", salvo.getStatus().name()),
                        principal.usuarioId()));

        auditLog.log(
                principal.assessoriaId(),
                principal.usuarioId(),
                "PROSPECCAO",
                salvo.getId(),
                AuditLog.Acao.CREATE,
                Map.of("titulo", salvo.getTitulo(), "marcaId", salvo.getMarcaId().toString()));

        return salvo;
    }

    @Transactional
    public Prospeccao atualizar(
            AuthPrincipal principal,
            UUID id,
            UUID marcaId,
            UUID influenciadorId,
            UUID assessorResponsavelId,
            String titulo,
            Long valorEstimadoCentavos,
            String proximaAcao,
            LocalDate proximaAcaoEm,
            String observacoes,
            String[] tags) {
        Prospeccao p = buscar(principal, id);
        p.setMarcaId(marcaId);
        p.setInfluenciadorId(influenciadorId);
        p.setAssessorResponsavelId(
                assessorResponsavelId != null
                        ? assessorResponsavelId
                        : p.getAssessorResponsavelId());
        p.setTitulo(titulo);
        p.setValorEstimadoCentavos(valorEstimadoCentavos);
        p.setProximaAcao(proximaAcao);
        p.setProximaAcaoEm(proximaAcaoEm);
        p.setObservacoes(observacoes);
        if (tags != null) p.setTags(tags);
        p.setUpdatedAt(Instant.now());
        Prospeccao saved = repo.save(p);

        auditLog.log(
                principal.assessoriaId(),
                principal.usuarioId(),
                "PROSPECCAO",
                saved.getId(),
                AuditLog.Acao.UPDATE,
                Map.of("titulo", saved.getTitulo()));

        return saved;
    }

    @Transactional
    public Prospeccao mudarStatus(
            AuthPrincipal principal,
            UUID id,
            ProspeccaoStatus novo,
            MotivoPerda motivoPerda,
            String motivoPerdaDetalhe) {
        Prospeccao p = buscar(principal, id);
        ProspeccaoStatus antes = p.getStatus();
        ProspeccaoStateMachine.assertTransition(antes, novo);

        if (novo == ProspeccaoStatus.FECHADA_PERDIDA) {
            if (motivoPerda == null) {
                throw BusinessException.unprocessable(
                        "MOTIVO_PERDA_OBRIGATORIO",
                        "Informe motivo_perda ao fechar como FECHADA_PERDIDA.");
            }
            if (motivoPerda == MotivoPerda.OUTRO
                    && (motivoPerdaDetalhe == null || motivoPerdaDetalhe.isBlank())) {
                throw BusinessException.unprocessable(
                        "MOTIVO_PERDA_DETALHE_OBRIGATORIO",
                        "Quando motivo_perda = OUTRO, detalhe é obrigatório.");
            }
            p.setMotivoPerda(motivoPerda);
            p.setMotivoPerdaDetalhe(motivoPerdaDetalhe);
        } else if (novo == ProspeccaoStatus.NOVA && antes == ProspeccaoStatus.FECHADA_PERDIDA) {
            // Reabrindo: limpa motivo
            p.setMotivoPerda(null);
            p.setMotivoPerdaDetalhe(null);
            p.setFechadaEm(null);
        }

        p.setStatus(novo);
        if (novo.isFechada()) {
            p.setFechadaEm(Instant.now());
        }
        p.setUpdatedAt(Instant.now());
        Prospeccao saved = repo.save(p);

        Counter.builder("prospeccao_status_change_total")
                .tag("de", antes.name())
                .tag("para", novo.name())
                .register(meterRegistry)
                .increment();

        if (novo.isFechada()) {
            Counter.builder("prospeccoes_fechadas_total")
                    .tag("resultado", novo == ProspeccaoStatus.FECHADA_GANHA ? "ganha" : "perdida")
                    .register(meterRegistry)
                    .increment();

            if (novo == ProspeccaoStatus.FECHADA_GANHA && saved.getCreatedAt() != null) {
                long dias = ChronoUnit.DAYS.between(saved.getCreatedAt(), Instant.now());
                Timer.builder("prospeccao_time_to_close")
                        .description("Dias entre criação e FECHADA_GANHA")
                        .register(meterRegistry)
                        .record(Duration.ofDays(dias));
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("de", antes.name());
        payload.put("para", novo.name());
        if (motivoPerda != null) payload.put("motivo_perda", motivoPerda.name());
        if (motivoPerdaDetalhe != null) payload.put("motivo_perda_detalhe", motivoPerdaDetalhe);

        eventoRepo.save(
                new ProspeccaoEvento(
                        saved.getId(),
                        principal.assessoriaId(),
                        EventoTipo.STATUS_CHANGE,
                        payload,
                        principal.usuarioId()));

        eventPublisher.publishEvent(new ProspeccaoMudouStatusEvent(
                principal.assessoriaId(),
                saved.getAssessorResponsavelId(),
                saved.getId(),
                saved.getTitulo(),
                antes.name(),
                novo.name()));

        return saved;
    }

    @Transactional
    public void deletar(AuthPrincipal principal, UUID id) {
        Prospeccao p = buscar(principal, id);
        p.setDeletedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        repo.save(p);

        auditLog.log(
                principal.assessoriaId(),
                principal.usuarioId(),
                "PROSPECCAO",
                p.getId(),
                AuditLog.Acao.DELETE,
                Map.of("titulo", p.getTitulo()));
    }

    // ─── Comentários ────────────────────────────────────────────────────────
    @Transactional
    public ProspeccaoEvento adicionarComentario(
            AuthPrincipal principal, UUID prospeccaoId, String texto) {
        Prospeccao p = buscar(principal, prospeccaoId);
        if (texto == null || texto.isBlank()) {
            throw BusinessException.badRequest("COMENTARIO_VAZIO", "Texto obrigatório.");
        }
        return eventoRepo.save(
                new ProspeccaoEvento(
                        p.getId(),
                        principal.assessoriaId(),
                        EventoTipo.COMMENT,
                        Map.of("texto", texto),
                        principal.usuarioId()));
    }

    @Transactional(readOnly = true)
    public java.util.List<ProspeccaoEvento> eventos(AuthPrincipal principal, UUID prospeccaoId) {
        buscar(principal, prospeccaoId); // valida visibilidade
        return eventoRepo.findByProspeccaoIdOrderByCreatedAtDesc(prospeccaoId);
    }

    // ─── Dashboard ─────────────────────────────────────────────────────────
    public record DashboardSummary(
            java.util.Map<ProspeccaoStatus, Long> porStatus,
            long fechadasMes,
            double taxaConversao,
            double timeToCloseDiasMedio) {}

    @Transactional(readOnly = true)
    public DashboardSummary dashboard(AuthPrincipal principal) {
        var counts = repo.contarPorStatus(principal.assessoriaId());
        java.util.EnumMap<ProspeccaoStatus, Long> porStatus =
                new java.util.EnumMap<>(ProspeccaoStatus.class);
        for (ProspeccaoStatus s : ProspeccaoStatus.values()) porStatus.put(s, 0L);
        for (var c : counts) porStatus.put(c.getStatus(), c.getTotal());

        java.time.Instant inicioMes =
                java.time.LocalDate.now()
                        .withDayOfMonth(1)
                        .atStartOfDay(java.time.ZoneOffset.UTC)
                        .toInstant();
        long fechadasMes = repo.countFechadasDesde(principal.assessoriaId(), inicioMes);

        long ganhas = porStatus.get(ProspeccaoStatus.FECHADA_GANHA);
        long perdidas = porStatus.get(ProspeccaoStatus.FECHADA_PERDIDA);
        double taxa = (ganhas + perdidas) == 0 ? 0d : (double) ganhas / (ganhas + perdidas);

        Double ttc = repo.timeToCloseDiasMedio(principal.assessoriaId());
        return new DashboardSummary(porStatus, fechadasMes, taxa, ttc != null ? ttc : 0d);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────
    private boolean canSeeAll(AuthPrincipal principal) {
        return "OWNER".equals(principal.role()) || principal.permissions().contains("OWNR");
    }

    private void ensureVisible(AuthPrincipal principal, Prospeccao p) {
        if (!p.getAssessoriaId().equals(principal.assessoriaId())) {
            throw BusinessException.notFound("PROSPECCAO");
        }
        if (canSeeAll(principal)) return;
        boolean ehResponsavel = principal.usuarioId().equals(p.getAssessorResponsavelId());
        boolean ehCriador =
                p.getCreatedBy() != null && principal.usuarioId().equals(p.getCreatedBy());
        if (!ehResponsavel && !ehCriador) {
            throw BusinessException.notFound("PROSPECCAO");
        }
    }
}
