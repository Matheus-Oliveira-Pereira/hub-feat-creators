package com.hubfeatcreators.domain.influenciador;

import com.hubfeatcreators.domain.historico.Evento.EntidadeRef;
import com.hubfeatcreators.domain.historico.EventoService;
import com.hubfeatcreators.domain.historico.EventoTipo;
import com.hubfeatcreators.infra.audit.AuditLog;
import com.hubfeatcreators.infra.audit.AuditLogService;
import com.hubfeatcreators.infra.job.JobService;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.web.BusinessException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InfluenciadorService {

    private static final String JOB_LGPD = "INFLUENCIADOR_LGPD_NOTIFY";

    private final InfluenciadorRepository repo;
    private final AuditLogService auditLogService;
    private final JobService jobService;
    private final EventoService eventoService;

    public InfluenciadorService(
            InfluenciadorRepository repo,
            AuditLogService auditLogService,
            JobService jobService,
            EventoService eventoService) {
        this.repo = repo;
        this.auditLogService = auditLogService;
        this.jobService = jobService;
        this.eventoService = eventoService;
    }

    @Transactional
    public Influenciador criar(AuthPrincipal principal, InfluenciadorRequest req) {
        Influenciador inf =
                new Influenciador(principal.assessoriaId(), req.nome(), principal.usuarioId());
        applyRequest(inf, req);
        inf = repo.save(inf);

        auditLogService.log(
                principal.assessoriaId(),
                principal.usuarioId(),
                "influenciador",
                inf.getId(),
                AuditLog.Acao.CREATE,
                toMap(inf));

        jobService.enqueue(
                principal.assessoriaId(),
                JOB_LGPD,
                Map.of(
                        "influenciadorId",
                        inf.getId().toString(),
                        "email",
                        req.handles().getOrDefault("email", "")),
                inf.getId());

        eventoService.registrar(
                principal.assessoriaId(),
                principal.usuarioId(),
                EventoTipo.INFLUENCIADOR_CRIADO,
                Map.of("nome", inf.getNome()),
                new EntidadeRef("INFLUENCIADOR", inf.getId()));

        return inf;
    }

    @Transactional(readOnly = true)
    public Page<Influenciador> listar(String nome, String nicho, int page, int size) {
        return repo.search(
                nome != null && nome.isBlank() ? null : nome,
                nicho,
                PageRequest.of(page, Math.min(size, 100)));
    }

    @Transactional(readOnly = true)
    public Influenciador buscar(UUID id) {
        return repo.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BusinessException.notFound("INFLUENCIADOR"));
    }

    @Transactional
    public Influenciador atualizar(AuthPrincipal principal, UUID id, InfluenciadorRequest req) {
        Influenciador inf = buscar(id);
        applyRequest(inf, req);
        inf.setUpdatedAt(Instant.now());
        inf = repo.save(inf);

        auditLogService.log(
                principal.assessoriaId(),
                principal.usuarioId(),
                "influenciador",
                id,
                AuditLog.Acao.UPDATE,
                toMap(inf));

        return inf;
    }

    @Transactional
    public void deletar(AuthPrincipal principal, UUID id) {
        Influenciador inf = buscar(id);
        inf.setDeletedAt(Instant.now());
        inf.setUpdatedAt(Instant.now());
        repo.save(inf);

        auditLogService.log(
                principal.assessoriaId(),
                principal.usuarioId(),
                "influenciador",
                id,
                AuditLog.Acao.DELETE,
                Map.of("id", id.toString()));
    }

    @Transactional(readOnly = true)
    public List<Influenciador> exportar() {
        return repo.findAllActiveForExport();
    }

    private void applyRequest(Influenciador inf, InfluenciadorRequest req) {
        inf.setNome(req.nome());
        inf.setHandles(req.handles() != null ? req.handles() : Map.of());
        inf.setNicho(req.nicho());
        inf.setAudienciaTotal(req.audienciaTotal());
        inf.setObservacoes(req.observacoes());
        inf.setTags(req.tags() != null ? req.tags().toArray(new String[0]) : new String[0]);
        inf.setBaseLegal(req.baseLegal());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Influenciador inf) {
        return Map.of(
                "id", inf.getId().toString(),
                "nome", inf.getNome(),
                "nicho", inf.getNicho() != null ? inf.getNicho() : "",
                "tags", Arrays.asList(inf.getTags()));
    }
}
