package com.hubfeatcreators.domain.notificacao;

import com.hubfeatcreators.config.AppProperties;
import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webpush")
public class WebPushController {

    private static final Logger log = LoggerFactory.getLogger(WebPushController.class);

    private final AppProperties appProperties;
    private final WebpushSubscriptionRepository subRepo;

    public WebPushController(AppProperties appProperties, WebpushSubscriptionRepository subRepo) {
        this.appProperties = appProperties;
        this.subRepo = subRepo;
    }

    @GetMapping("/public-key")
    public Map<String, String> publicKey() {
        return Map.of("publicKey", appProperties.getWebpush().getPublicKey());
    }

    @PostMapping("/subscribe")
    @RequirePermission(PermissionCodes.B_NOT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void subscribe(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody SubscribeRequest body,
            HttpServletRequest request) {
        UUID usuarioId = principal.usuarioId();
        subRepo.findByEndpoint(body.endpoint()).ifPresentOrElse(
                sub -> {
                    sub.setAtiva(true);
                    sub.setLastUsedAt(java.time.Instant.now());
                    subRepo.save(sub);
                },
                () -> {
                    WebpushSubscription sub = new WebpushSubscription(
                            usuarioId, body.endpoint(), body.p256dh(), body.auth(),
                            request.getHeader("User-Agent"));
                    subRepo.save(sub);
                    log.info("webpush.subscribe usuarioId={}", usuarioId);
                });
    }

    @DeleteMapping("/unsubscribe")
    @RequirePermission(PermissionCodes.B_NOT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unsubscribe(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody Map<String, String> body) {
        String endpoint = body.get("endpoint");
        if (endpoint != null) {
            subRepo.markInativa(endpoint);
            log.info("webpush.unsubscribe usuarioId={}", principal.usuarioId());
        }
    }

    public record SubscribeRequest(String endpoint, String p256dh, String auth) {}
}
