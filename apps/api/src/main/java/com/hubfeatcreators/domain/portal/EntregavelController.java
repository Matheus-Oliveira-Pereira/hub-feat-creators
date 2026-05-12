package com.hubfeatcreators.domain.portal;

import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portal/entregaveis")
public class EntregavelController {

    private final PortalService portalService;

    public EntregavelController(PortalService portalService) {
        this.portalService = portalService;
    }

    @PatchMapping("/{id}/revisao")
    @RequirePermission(PermissionCodes.C_TAR)
    public CreatorEntregavel revisar(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @RequestBody RevisaoRequest req) {
        return portalService.revisarEntregavel(principal.assessoriaId(), id, req.status(), req.feedback());
    }

    public record RevisaoRequest(String status, String feedback) {}
}
