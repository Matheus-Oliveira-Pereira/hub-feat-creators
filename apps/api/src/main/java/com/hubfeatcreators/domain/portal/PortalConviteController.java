package com.hubfeatcreators.domain.portal;

import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portal/convites")
public class PortalConviteController {

    private final CreatorAuthService authService;

    public PortalConviteController(CreatorAuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.C_INF)
    public CreatorInvite convidar(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody ConvidarRequest req) {
        return authService.convidar(principal, req.influenciadorId(), req.email());
    }

    public record ConvidarRequest(UUID influenciadorId, @Email String email) {}
}
