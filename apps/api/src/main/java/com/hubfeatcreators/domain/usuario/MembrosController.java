package com.hubfeatcreators.domain.usuario;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.audit.AuditLog;
import com.hubfeatcreators.infra.audit.AuditLogService;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import com.hubfeatcreators.infra.web.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/membros")
public class MembrosController {

    private final UsuarioRepository usuarioRepo;
    private final AuditLogService auditLogService;

    public MembrosController(UsuarioRepository usuarioRepo, AuditLogService auditLogService) {
        this.usuarioRepo = usuarioRepo;
        this.auditLogService = auditLogService;
    }

    record MembroResponse(
            UUID id, String email, String role, String status, boolean mfaAtivo,
            boolean emailVerificado, UUID profileId, Instant ultimoLoginEm, Instant createdAt) {}

    record AtualizarStatusRequest(String status) {}

    @GetMapping
    @RequirePermission(PermissionCodes.B_USU)
    public List<MembroResponse> listar(@AuthenticationPrincipal AuthPrincipal principal) {
        return usuarioRepo.findAllByAssessoriaId(principal.assessoriaId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @PatchMapping("/{id}/status")
    @RequirePermission(PermissionCodes.E_USU)
    public MembroResponse atualizarStatus(
            @PathVariable UUID id,
            @RequestBody AtualizarStatusRequest req,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest http) {
        Usuario membro = findInTenant(id, principal);

        if (membro.getId().equals(principal.usuarioId())) {
            throw BusinessException.badRequest("SELF_STATUS", "Não é possível alterar o próprio status.");
        }

        Usuario.Status novoStatus = parseStatus(req.status());
        Usuario.Status anterior = membro.getStatus();
        membro.setStatus(novoStatus);
        membro.setUpdatedAt(Instant.now());
        usuarioRepo.save(membro);

        AuditLog.Acao acao = novoStatus == Usuario.Status.ATIVO
                ? AuditLog.Acao.MEMBER_ACTIVATED : AuditLog.Acao.MEMBER_DEACTIVATED;
        auditLogService.logAuth(principal.assessoriaId(), principal.usuarioId(), acao,
                Map.of("membroId", id, "de", anterior.name(), "para", novoStatus.name()),
                ip(http), ua(http));

        return toResponse(membro);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.D_USU)
    public void remover(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest http) {
        if (id.equals(principal.usuarioId())) {
            throw BusinessException.badRequest("SELF_REMOVE", "Não é possível remover a si mesmo.");
        }

        Usuario membro = findInTenant(id, principal);
        membro.setDeletedAt(Instant.now());
        membro.setUpdatedAt(Instant.now());
        usuarioRepo.save(membro);

        auditLogService.logAuth(principal.assessoriaId(), principal.usuarioId(),
                AuditLog.Acao.MEMBER_REMOVED,
                Map.of("membroId", id, "email", membro.getEmail()),
                ip(http), ua(http));
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Usuario findInTenant(UUID id, AuthPrincipal principal) {
        return usuarioRepo.findByIdAndAssessoriaId(id, principal.assessoriaId())
                .orElseThrow(() -> BusinessException.notFound("USUARIO"));
    }

    private MembroResponse toResponse(Usuario u) {
        return new MembroResponse(
                u.getId(), u.getEmail(), u.getRole().name(), u.getStatus().name(),
                u.isMfaAtivo(), u.isEmailVerificado(), u.getProfileId(),
                u.getUltimoLoginEm(), u.getCreatedAt());
    }

    private static Usuario.Status parseStatus(String s) {
        try {
            return Usuario.Status.valueOf(s.toUpperCase());
        } catch (Exception e) {
            throw BusinessException.badRequest("STATUS_INVALIDO", "Status inválido: " + s);
        }
    }

    private static String ip(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        return xff != null ? xff.split(",")[0].trim() : req.getRemoteAddr();
    }

    private static String ua(HttpServletRequest req) { return req.getHeader("User-Agent"); }
}
