package com.hubfeatcreators.domain.notificacao;

import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.CreatorPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/devices")
public class DeviceSubscriptionController {

    private final DeviceSubscriptionService service;

    public DeviceSubscriptionController(DeviceSubscriptionService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody RegisterRequest req) {
        UUID userId = resolveUserId(principal);
        String userTipo = resolveUserTipo(principal);
        service.register(userId, userTipo, req.canal(), req.token(), req.plataforma());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{token}")
    public ResponseEntity<Void> unregister(@PathVariable String token) {
        service.unregister(token);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveUserId(Object principal) {
        if (principal instanceof CreatorPrincipal cp) return cp.creatorUserId();
        if (principal instanceof AuthPrincipal ap) return ap.usuarioId();
        throw new IllegalStateException("Unknown principal type");
    }

    private String resolveUserTipo(Object principal) {
        if (principal instanceof CreatorPrincipal) return "CREATOR";
        return "INTERNO";
    }

    public record RegisterRequest(
            @NotBlank @Pattern(regexp = "APNS|FCM") String canal,
            @NotBlank String token,
            String plataforma) {}
}
