package com.hubfeatcreators.domain.admin;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/feature-flags")
@RequirePermission(PermissionCodes.ADMN)
public class AdminFeatureFlagController {

    private final PlatformFeatureFlagService service;
    private final AdminAuditService auditService;

    public AdminFeatureFlagController(
            PlatformFeatureFlagService service, AdminAuditService auditService) {
        this.service = service;
        this.auditService = auditService;
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
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest http) {
        PlatformFeatureFlag flag = service.toggle(key, req.enabled(), principal.usuarioId());
        auditService.log(
                principal.usuarioId(),
                "FEATURE_FLAG_CHANGE",
                null,
                "feature_flag",
                null,
                null,
                "{\"key\":\"" + key + "\",\"enabled\":" + req.enabled() + "}",
                ip(http),
                http.getHeader("User-Agent"));
        return new FeatureFlagResponse(flag.getKey(), flag.isEnabled());
    }

    private String ip(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        return xff != null ? xff.split(",")[0].trim() : req.getRemoteAddr();
    }
}
