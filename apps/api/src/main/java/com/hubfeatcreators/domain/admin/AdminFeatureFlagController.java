package com.hubfeatcreators.domain.admin;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/configuracoes/feature-flags")
@RequirePermission(PermissionCodes.FLAG)
public class AdminFeatureFlagController {

    private final PlatformFeatureFlagService service;

    public AdminFeatureFlagController(PlatformFeatureFlagService service) {
        this.service = service;
    }

    record FeatureFlagResponse(String key, boolean enabled) {}

    record ToggleRequest(@NotNull Boolean enabled) {}

    @GetMapping
    public List<FeatureFlagResponse> list() {
        return service.findAll().stream()
                .map(f -> new FeatureFlagResponse(f.getKey(), f.isEnabled()))
                .toList();
    }

    @PutMapping("/{key}")
    public FeatureFlagResponse toggle(
            @PathVariable String key,
            @Valid @RequestBody ToggleRequest req,
            @AuthenticationPrincipal AuthPrincipal principal) {
        PlatformFeatureFlag flag = service.toggle(key, req.enabled(), principal.usuarioId());
        return new FeatureFlagResponse(flag.getKey(), flag.isEnabled());
    }
}
