package com.hubfeatcreators.domain.marca;

import com.hubfeatcreators.infra.audit.AuditLog;
import com.hubfeatcreators.infra.audit.AuditLogService;
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
public class MarcaService {

    private final MarcaRepository repo;
    private final AuditLogService auditLogService;

    public MarcaService(MarcaRepository repo, AuditLogService auditLogService) {
        this.repo = repo;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Marca criar(AuthPrincipal principal, MarcaRequest req) {
        Marca marca = new Marca(principal.assessoriaId(), req.nome(), principal.usuarioId());
        applyRequest(marca, req);
        marca = repo.save(marca);
        auditLogService.log(
                principal.assessoriaId(),
                principal.usuarioId(),
                "marca",
                marca.getId(),
                AuditLog.Acao.CREATE,
                toMap(marca));
        return marca;
    }

    @Transactional(readOnly = true)
    public Page<Marca> listar(String nome, String segmento, int page, int size) {
        return repo.search(
                nome != null && nome.isBlank() ? null : nome,
                segmento,
                PageRequest.of(page, Math.min(size, 100)));
    }

    @Transactional(readOnly = true)
    public Marca buscar(UUID id) {
        return repo.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BusinessException.notFound("MARCA"));
    }

    @Transactional
    public Marca atualizar(AuthPrincipal principal, UUID id, MarcaRequest req) {
        Marca marca = buscar(id);
        applyRequest(marca, req);
        marca.setUpdatedAt(Instant.now());
        marca = repo.save(marca);
        auditLogService.log(
                principal.assessoriaId(),
                principal.usuarioId(),
                "marca",
                id,
                AuditLog.Acao.UPDATE,
                toMap(marca));
        return marca;
    }

    @Transactional
    public void deletar(AuthPrincipal principal, UUID id) {
        Marca marca = buscar(id);
        marca.setDeletedAt(Instant.now());
        marca.setUpdatedAt(Instant.now());
        repo.save(marca);
        auditLogService.log(
                principal.assessoriaId(),
                principal.usuarioId(),
                "marca",
                id,
                AuditLog.Acao.DELETE,
                Map.of("id", id.toString()));
    }

    @Transactional(readOnly = true)
    public List<Marca> exportar() {
        return repo.findAllActiveForExport();
    }

    private void applyRequest(Marca marca, MarcaRequest req) {
        marca.setNome(req.nome());
        marca.setSegmento(req.segmento());
        marca.setSite(req.site());
        marca.setObservacoes(req.observacoes());
        marca.setTags(req.tags() != null ? req.tags().toArray(new String[0]) : new String[0]);
    }

    private Map<String, Object> toMap(Marca m) {
        return Map.of(
                "id",
                m.getId().toString(),
                "nome",
                m.getNome(),
                "segmento",
                m.getSegmento() != null ? m.getSegmento() : "",
                "tags",
                Arrays.asList(m.getTags()));
    }
}
