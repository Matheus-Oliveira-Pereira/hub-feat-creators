package com.hubfeatcreators.domain.tarefa;

import com.hubfeatcreators.domain.historico.Evento.EntidadeRef;
import com.hubfeatcreators.domain.historico.EventoService;
import com.hubfeatcreators.domain.historico.EventoTipo;
import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.audit.AuditLog;
import com.hubfeatcreators.infra.audit.AuditLogService;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.web.BusinessException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TarefaService {

    private static final int MAX_PAGE = 100;
    private static final ZoneId TZ_BR = ZoneId.of("America/Sao_Paulo");

    private final TarefaRepository repo;
    private final TarefaComentarioRepository comentarioRepo;
    private final UsuarioPreferenciaRepository prefRepo;
    private final MeterRegistry meterRegistry;
    private final AuditLogService auditLog;
    private final EventoService eventoService;

    public TarefaService(
            TarefaRepository repo,
            TarefaComentarioRepository comentarioRepo,
            UsuarioPreferenciaRepository prefRepo,
            MeterRegistry meterRegistry,
            AuditLogService auditLog,
            EventoService eventoService) {
        this.repo = repo;
        this.comentarioRepo = comentarioRepo;
        this.prefRepo = prefRepo;
        this.meterRegistry = meterRegistry;
        this.auditLog = auditLog;
        this.eventoService = eventoService;
    }

    // ─── Read ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<Tarefa> listar(
            AuthPrincipal principal,
            TarefaStatus status,
            TarefaPrioridade prioridade,
            UUID responsavelId,
            String prazoFiltro,
            boolean minhas,
            int page,
            int size) {
        var pageable =
                PageRequest.of(
                        Math.max(0, page), Math.min(size, MAX_PAGE), Sort.by("prazo").ascending());

        UUID resp = minhas ? principal.usuarioId() : responsavelId;
        Instant agora = Instant.now();
        ZonedDateTime inicioDiaBr = ZonedDateTime.now(TZ_BR).toLocalDate().atStartOfDay(TZ_BR);
        Instant inicioDia = inicioDiaBr.toInstant();
        Instant fimDia = inicioDiaBr.plusDays(1).toInstant();
        Instant fimSemana = inicioDiaBr.plusDays(7).toInstant();

        return repo.findAllFiltered(
                principal.assessoriaId(),
                status,
                prioridade,
                resp,
                prazoFiltro,
                agora,
                inicioDia,
                fimDia,
                fimSemana,
                pageable);
    }

    @Transactional(readOnly = true)
    public Tarefa buscar(AuthPrincipal principal, UUID id) {
        Tarefa t = repo.findById(id).orElseThrow(() -> BusinessException.notFound("TAREFA"));
        ensureTenant(principal, t);
        return t;
    }

    @Transactional(readOnly = true)
    public long contarAlerta(AuthPrincipal principal) {
        ZonedDateTime inicioDiaBr = ZonedDateTime.now(TZ_BR).toLocalDate().atStartOfDay(TZ_BR);
        Instant fimDia = inicioDiaBr.plusDays(1).toInstant();
        return repo.countAlerta(principal.assessoriaId(), principal.usuarioId(), fimDia);
    }

    @Transactional(readOnly = true)
    public List<Tarefa> listarPorEntidade(
            AuthPrincipal principal, EntidadeTipo tipo, UUID entidadeId) {
        return repo.findByEntidade(principal.assessoriaId(), tipo, entidadeId);
    }

    // ─── Write ──────────────────────────────────────────────────────────────

    @Transactional
    public Tarefa criar(
            AuthPrincipal principal,
            String titulo,
            String descricao,
            Instant prazo,
            TarefaPrioridade prioridade,
            UUID responsavelId,
            EntidadeTipo entidadeTipo,
            UUID entidadeId) {

        UUID respId = resolveResponsavel(principal, responsavelId);
        Tarefa t =
                new Tarefa(principal.assessoriaId(), titulo, prazo, respId, principal.usuarioId());
        t.setDescricao(descricao);
        if (prioridade != null) t.setPrioridade(prioridade);
        if (entidadeTipo != null) {
            t.setEntidadeTipo(entidadeTipo);
            t.setEntidadeId(entidadeId);
        }

        Tarefa saved = repo.save(t);

        Counter.builder("tarefas_criadas_total").register(meterRegistry).increment();

        auditLog.log(
                principal.assessoriaId(),
                principal.usuarioId(),
                "TAREFA",
                saved.getId(),
                AuditLog.Acao.CREATE,
                Map.of(
                        "titulo",
                        saved.getTitulo(),
                        "responsavelId",
                        saved.getResponsavelId().toString()));

        EntidadeRef tarefaRef = new EntidadeRef("TAREFA", saved.getId());
        if (saved.getEntidadeId() != null && saved.getEntidadeTipo() != null) {
            eventoService.registrar(
                    principal.assessoriaId(),
                    principal.usuarioId(),
                    EventoTipo.TAREFA_CRIADA,
                    Map.of("titulo", saved.getTitulo()),
                    tarefaRef,
                    new EntidadeRef(saved.getEntidadeTipo().name(), saved.getEntidadeId()));
        } else {
            eventoService.registrar(
                    principal.assessoriaId(),
                    principal.usuarioId(),
                    EventoTipo.TAREFA_CRIADA,
                    Map.of("titulo", saved.getTitulo()),
                    tarefaRef);
        }

        return saved;
    }

    @Transactional
    public Tarefa atualizar(
            AuthPrincipal principal,
            UUID id,
            String titulo,
            String descricao,
            Instant prazo,
            TarefaPrioridade prioridade,
            UUID responsavelId,
            EntidadeTipo entidadeTipo,
            UUID entidadeId) {

        Tarefa t = buscar(principal, id);
        if (t.getStatus().isTerminal()) {
            throw BusinessException.unprocessable(
                    "TAREFA_TERMINAL", "Não é possível editar tarefa finalizada.");
        }

        if (titulo != null) t.setTitulo(titulo);
        if (descricao != null) t.setDescricao(descricao);
        if (prazo != null) t.setPrazo(prazo);
        if (prioridade != null) t.setPrioridade(prioridade);
        if (responsavelId != null) t.setResponsavelId(resolveResponsavel(principal, responsavelId));
        if (entidadeTipo != null) {
            t.setEntidadeTipo(entidadeTipo);
            t.setEntidadeId(entidadeId);
        }
        t.setUpdatedAt(Instant.now());

        Tarefa saved = repo.save(t);
        auditLog.log(
                principal.assessoriaId(),
                principal.usuarioId(),
                "TAREFA",
                saved.getId(),
                AuditLog.Acao.UPDATE,
                Map.of("titulo", saved.getTitulo()));

        return saved;
    }

    @Transactional
    public Tarefa mudarStatus(AuthPrincipal principal, UUID id, TarefaStatus novoStatus) {
        Tarefa t = buscar(principal, id);
        TarefaStatus antes = t.getStatus();

        t.setStatus(novoStatus);
        if (novoStatus == TarefaStatus.FEITA) {
            t.setConcluidaEm(Instant.now());
            Counter.builder("tarefas_concluidas_total").register(meterRegistry).increment();
        } else if (antes == TarefaStatus.FEITA) {
            // Reabrindo: limpa concluidaEm
            t.setConcluidaEm(null);
        }
        t.setUpdatedAt(Instant.now());

        Tarefa saved = repo.save(t);
        auditLog.log(
                principal.assessoriaId(),
                principal.usuarioId(),
                "TAREFA",
                saved.getId(),
                AuditLog.Acao.UPDATE,
                Map.of("de", antes.name(), "para", novoStatus.name()));

        if (novoStatus == TarefaStatus.FEITA) {
            eventoService.registrar(
                    principal.assessoriaId(),
                    principal.usuarioId(),
                    EventoTipo.TAREFA_CONCLUIDA,
                    Map.of("titulo", saved.getTitulo()),
                    new EntidadeRef("TAREFA", saved.getId()));
        }

        return saved;
    }

    @Transactional
    public void deletar(AuthPrincipal principal, UUID id) {
        Tarefa t = buscar(principal, id);
        t.setDeletedAt(Instant.now());
        t.setUpdatedAt(Instant.now());
        repo.save(t);

        auditLog.log(
                principal.assessoriaId(),
                principal.usuarioId(),
                "TAREFA",
                t.getId(),
                AuditLog.Acao.DELETE,
                Map.of("titulo", t.getTitulo()));
    }

    // ─── Comentários ────────────────────────────────────────────────────────

    @Transactional
    public TarefaComentario adicionarComentario(
            AuthPrincipal principal, UUID tarefaId, String texto) {
        Tarefa t = buscar(principal, tarefaId);
        if (texto == null || texto.isBlank()) {
            throw BusinessException.badRequest("COMENTARIO_VAZIO", "Texto obrigatório.");
        }
        return comentarioRepo.save(
                new TarefaComentario(
                        t.getId(), principal.assessoriaId(), principal.usuarioId(), texto));
    }

    @Transactional(readOnly = true)
    public List<TarefaComentario> comentarios(AuthPrincipal principal, UUID tarefaId) {
        buscar(principal, tarefaId);
        return comentarioRepo.findByTarefaIdOrderByCreatedAtDesc(tarefaId);
    }

    // ─── Preferências ───────────────────────────────────────────────────────

    @Transactional
    public UsuarioPreferencia atualizarPreferencias(
            AuthPrincipal principal, boolean digestEnabled) {
        UsuarioPreferencia pref =
                prefRepo.findById(principal.usuarioId())
                        .orElse(new UsuarioPreferencia(principal.usuarioId()));
        pref.setDigestDiarioEnabled(digestEnabled);
        pref.setUpdatedAt(Instant.now());
        return prefRepo.save(pref);
    }

    @Transactional(readOnly = true)
    public UsuarioPreferencia preferencias(AuthPrincipal principal) {
        return prefRepo.findById(principal.usuarioId())
                .orElse(new UsuarioPreferencia(principal.usuarioId()));
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private void ensureTenant(AuthPrincipal principal, Tarefa t) {
        if (!t.getAssessoriaId().equals(principal.assessoriaId())) {
            throw BusinessException.notFound("TAREFA");
        }
    }

    /**
     * Resolve responsavelId conforme AC-2: - OWNER pode atribuir a qualquer usuário da assessoria -
     * ASSESSOR: só pode auto-atribuir OU devolver ao OWNER (responsavelId = null → próprio)
     */
    private UUID resolveResponsavel(AuthPrincipal principal, UUID responsavelId) {
        if (responsavelId == null) return principal.usuarioId();

        boolean isOwner =
                principal.hasPermission(PermissionCodes.OWNR) || "OWNER".equals(principal.role());
        if (!isOwner && !responsavelId.equals(principal.usuarioId())) {
            throw BusinessException.unprocessable(
                    "ATRIBUICAO_NEGADA",
                    "ASSESSOR só pode se auto-atribuir ou devolver tarefa. Use responsavel_id = seu próprio ID.");
        }
        return responsavelId;
    }
}
