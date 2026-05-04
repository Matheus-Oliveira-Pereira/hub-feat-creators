package com.hubfeatcreators.domain.convite;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ConviteController {

    private final ConviteService conviteService;

    public ConviteController(ConviteService conviteService) {
        this.conviteService = conviteService;
    }

    record ConviteRequest(
            @NotBlank @Email String email,
            String role,
            UUID perfilId) {}

    record AceitarRequest(@NotBlank String token, @NotBlank @Size(min = 8) String senha) {}

    record ConviteResponse(UUID id, String email, String role, UUID perfilId, Instant expiresAt) {}

    record UsuarioResponse(UUID id, String email, String role) {}

    @PostMapping("/convites")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.INVT)
    public ConviteResponse convidar(
            @Valid @RequestBody ConviteRequest req,
            @AuthenticationPrincipal AuthPrincipal principal) {
        Convite.Role role = req.role() != null ? Convite.Role.valueOf(req.role()) : Convite.Role.ASSESSOR;
        Convite c = conviteService.convidar(principal, req.email(), role, req.perfilId());
        return new ConviteResponse(c.getId(), c.getEmail(), c.getRole().name(), c.getPerfilId(), c.getExpiresAt());
    }

    @GetMapping("/convites")
    @RequirePermission(PermissionCodes.INVT)
    public List<ConviteResponse> listar(@AuthenticationPrincipal AuthPrincipal principal) {
        return conviteService.listar(principal).stream()
                .map(c -> new ConviteResponse(c.getId(), c.getEmail(), c.getRole().name(), c.getPerfilId(), c.getExpiresAt()))
                .toList();
    }

    @PostMapping("/auth/aceitar-convite")
    public UsuarioResponse aceitar(@Valid @RequestBody AceitarRequest req) {
        var u = conviteService.aceitarConvite(req.token(), req.senha());
        return new UsuarioResponse(u.getId(), u.getEmail(), u.getRole().name());
    }
}
