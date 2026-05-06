package com.hubfeatcreators.domain.contato;

import com.hubfeatcreators.domain.historico.Evento.EntidadeRef;
import com.hubfeatcreators.domain.historico.EventoService;
import com.hubfeatcreators.domain.historico.EventoTipo;
import com.hubfeatcreators.infra.audit.AuditLog;
import com.hubfeatcreators.infra.audit.AuditLogService;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.web.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContatoService {

    private final ContatoRepository repo;
    private final AuditLogService auditLogService;
    private final EventoService eventoService;

    public ContatoService(
            ContatoRepository repo, AuditLogService auditLogService, EventoService eventoService) {
        this.repo = repo;
        this.auditLogService = auditLogService;
        this.eventoService = eventoService;
    }

    @Transactional
    public Contato criar(AuthPrincipal principal, ContatoRequest req) {
        Contato contato = new Contato(req.marcaId(), principal.assessoriaId(), req.nome());
        applyRequest(contato, req);
        contato = repo.save(contato);
        auditLogService.log(
                principal.assessoriaId(),
                principal.usuarioId(),
                "contato",
                contato.getId(),
                AuditLog.Acao.CREATE,
                toMap(contato));
        eventoService.registrar(
                principal.assessoriaId(),
                principal.usuarioId(),
                EventoTipo.CONTATO_CRIADO,
                Map.of("nome", contato.getNome()),
                new EntidadeRef("CONTATO", contato.getId()),
                new EntidadeRef("MARCA", contato.getMarcaId()));
        return contato;
    }

    @Transactional(readOnly = true)
    public List<Contato> listarPorMarca(UUID marcaId) {
        return repo.findByMarcaIdAndDeletedAtIsNull(marcaId);
    }

    @Transactional(readOnly = true)
    public Contato buscar(UUID id) {
        return repo.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BusinessException.notFound("CONTATO"));
    }

    @Transactional
    public Contato atualizar(AuthPrincipal principal, UUID id, ContatoRequest req) {
        Contato contato = buscar(id);
        applyRequest(contato, req);
        contato.setUpdatedAt(Instant.now());
        contato = repo.save(contato);
        auditLogService.log(
                principal.assessoriaId(),
                principal.usuarioId(),
                "contato",
                id,
                AuditLog.Acao.UPDATE,
                toMap(contato));
        return contato;
    }

    @Transactional
    public void deletar(AuthPrincipal principal, UUID id) {
        Contato contato = buscar(id);
        contato.setDeletedAt(Instant.now());
        contato.setUpdatedAt(Instant.now());
        repo.save(contato);
        auditLogService.log(
                principal.assessoriaId(),
                principal.usuarioId(),
                "contato",
                id,
                AuditLog.Acao.DELETE,
                Map.of("id", id.toString()));
    }

    private void applyRequest(Contato c, ContatoRequest req) {
        c.setNome(req.nome());
        c.setEmail(req.email());
        c.setTelefone(req.telefone());
        c.setCargo(req.cargo());
        c.setBaseLegal(req.baseLegal());
    }

    private Map<String, Object> toMap(Contato c) {
        return Map.of(
                "id",
                c.getId().toString(),
                "nome",
                c.getNome(),
                "email",
                c.getEmail() != null ? c.getEmail() : "");
    }
}
