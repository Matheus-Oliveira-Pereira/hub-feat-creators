package com.hubfeatcreators.domain.admin;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.domain.usuario.Usuario;
import com.hubfeatcreators.domain.usuario.UsuarioRepository;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import com.hubfeatcreators.infra.web.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
@RequirePermission(PermissionCodes.ADMN)
public class AdminUsuarioController {

    private final UsuarioRepository usuarioRepo;
    private final AdminAuditService auditService;

    public AdminUsuarioController(UsuarioRepository usuarioRepo, AdminAuditService auditService) {
        this.usuarioRepo = usuarioRepo;
        this.auditService = auditService;
    }

    record UsuarioItem(
            UUID id,
            String email,
            String tipo,
            String role,
            String status,
            UUID assessoriaId,
            Instant createdAt) {}

    record PagedResponse(List<UsuarioItem> items, long total, int page, int size) {}

    record StatusRequest(@NotNull Usuario.Status status) {}

    @GetMapping
    public PagedResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        int safeSize = Math.min(size, 200);
        Page<Usuario> result =
                usuarioRepo.findAllActive(
                        PageRequest.of(page, safeSize, Sort.by("createdAt").descending()));
        List<UsuarioItem> items =
                result.getContent().stream()
                        .map(
                                u ->
                                        new UsuarioItem(
                                                u.getId(),
                                                u.getEmail(),
                                                u.getTipo() != null
                                                        ? u.getTipo().name()
                                                        : "INTERNO",
                                                u.getRole().name(),
                                                u.getStatus().name(),
                                                u.getAssessoriaId(),
                                                u.getCreatedAt()))
                        .toList();
        return new PagedResponse(items, result.getTotalElements(), page, safeSize);
    }

    @PutMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusRequest req,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest http) {
        Usuario u =
                usuarioRepo
                        .findById(id)
                        .filter(x -> x.getDeletedAt() == null)
                        .orElseThrow(() -> BusinessException.notFound("USUARIO"));

        String before = "{\"status\":\"" + u.getStatus().name() + "\"}";
        u.setStatus(req.status());
        u.setUpdatedAt(Instant.now());
        usuarioRepo.save(u);

        auditService.log(
                principal.usuarioId(),
                "USUARIO_EDIT",
                u.getAssessoriaId(),
                "usuario",
                id,
                before,
                null,
                ip(http),
                http.getHeader("User-Agent"));
    }

    private String ip(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        return xff != null ? xff.split(",")[0].trim() : req.getRemoteAddr();
    }
}
