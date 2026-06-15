package com.hubfeatcreators.domain.email;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/configuracoes/email")
public class SystemEmailConfigController {

    private final SystemEmailConfigService service;

    public SystemEmailConfigController(SystemEmailConfigService service) {
        this.service = service;
    }

    // ── DTOs ──────────────────────────────────────────────────────────────

    record ConfigResponse(
            String host,
            int port,
            String username,
            String fromAddress,
            String fromName,
            String tlsMode,
            int dailyQuota,
            String status,
            boolean passwordSet,
            Instant updatedAt) {}

    record ConfigUpdateRequest(
            String host,
            Integer port,
            String username,
            String password,
            @Email String fromAddress,
            String fromName,
            String tlsMode,
            Integer dailyQuota) {}

    record TestResponse(String message) {}

    // ── Endpoints ─────────────────────────────────────────────────────────

    @GetMapping
    @RequirePermission(PermissionCodes.OWNR)
    public ConfigResponse get() {
        return service.getEffectiveConfig().map(this::toResponse).orElse(emptyResponse());
    }

    @PatchMapping
    @RequirePermission(PermissionCodes.OWNR)
    public ConfigResponse update(@Valid @RequestBody ConfigUpdateRequest req) {
        SystemEmailConfig saved =
                service.salvar(
                        req.host(),
                        req.port(),
                        req.username(),
                        req.password(),
                        req.fromAddress(),
                        req.fromName(),
                        req.tlsMode(),
                        req.dailyQuota());
        return toResponse(saved);
    }

    @PostMapping("/test")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.OWNR)
    public void test() {
        service.testarConexao();
    }

    // ── Mappers ───────────────────────────────────────────────────────────

    private ConfigResponse toResponse(SystemEmailConfig c) {
        return new ConfigResponse(
                c.getHost(),
                c.getPort(),
                c.getUsername(),
                c.getFromAddress(),
                c.getFromName(),
                c.getTlsMode(),
                c.getDailyQuota(),
                c.getStatus(),
                c.getPasswordEnc() != null && c.getPasswordEnc().length > 0,
                c.getUpdatedAt());
    }

    private ConfigResponse emptyResponse() {
        return new ConfigResponse(
                "", 587, "", "", "feat. creators", "STARTTLS", 500, "NAO_CONFIGURADO", false, null);
    }
}
