package com.hubfeatcreators.domain.email;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public (no auth) endpoints for email tracking. Open pixel: 1x1 GIF. Click: redirect + record.
 * Unsubscribe: one-click (RFC 8058) + page.
 */
@RestController
@RequestMapping("/api/v1/email")
public class EmailTrackingController {

    private static final Logger log = LoggerFactory.getLogger(EmailTrackingController.class);

    // 1x1 transparent GIF
    private static final byte[] PIXEL_GIF =
            Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");

    private final EmailEnvioRepository envioRepo;
    private final EmailEventoRepository eventoRepo;
    private final EmailOptoutRepository optoutRepo;

    public EmailTrackingController(
            EmailEnvioRepository envioRepo,
            EmailEventoRepository eventoRepo,
            EmailOptoutRepository optoutRepo) {
        this.envioRepo = envioRepo;
        this.eventoRepo = eventoRepo;
        this.optoutRepo = optoutRepo;
    }

    /** Open-tracking pixel. Idempotent: only records first open per envio. */
    @GetMapping("/track/open/{envioId}")
    public ResponseEntity<byte[]> trackOpen(@PathVariable UUID envioId) {
        envioRepo
                .findById(envioId)
                .ifPresent(
                        envio -> {
                            if (!envio.isTrackingEnabled()) return;
                            boolean alreadyOpened =
                                    eventoRepo
                                            .findFirstByEnvioIdAndTipo(
                                                    envioId, EmailEventoTipo.ABERTO)
                                            .isPresent();
                            if (!alreadyOpened) {
                                eventoRepo.save(
                                        new EmailEvento(
                                                envioId,
                                                envio.getAssessoriaId(),
                                                EmailEventoTipo.ABERTO,
                                                Map.of()));
                                log.info("email.open envioId={}", envioId);
                            }
                        });
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "image/gif")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .body(PIXEL_GIF);
    }

    /** Click tracking: record CLICADO event then redirect to target URL. */
    @GetMapping("/track/click/{envioId}")
    public ResponseEntity<Void> trackClick(@PathVariable UUID envioId, @RequestParam String url) {
        envioRepo
                .findById(envioId)
                .ifPresent(
                        envio -> {
                            if (!envio.isTrackingEnabled()) return;
                            eventoRepo.save(
                                    new EmailEvento(
                                            envioId,
                                            envio.getAssessoriaId(),
                                            EmailEventoTipo.CLICADO,
                                            Map.of("url", url)));
                            log.info("email.click envioId={} url={}", envioId, url);
                        });
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    /**
     * One-click unsubscribe (RFC 8058 POST) and GET landing page. Token =
     * base64url(assessoriaId:email).
     */
    @PostMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribePost(@RequestParam String token) {
        processUnsubscribe(token);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribeGet(@RequestParam String token) {
        processUnsubscribe(token);
        String html =
                """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head><meta charset="UTF-8"><title>Descadastro confirmado</title></head>
        <body style="font-family:sans-serif;text-align:center;padding:80px">
          <h1>Descadastro confirmado</h1>
          <p>Você não receberá mais e-mails desta assessoria.</p>
        </body>
        </html>
        """;
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
                .body(html);
    }

    private void processUnsubscribe(String token) {
        try {
            String decoded =
                    new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            int sep = decoded.indexOf(':');
            if (sep < 1) return;
            UUID assessoriaId = UUID.fromString(decoded.substring(0, sep));
            String email = decoded.substring(sep + 1);

            if (!optoutRepo.existsByAssessoriaIdAndEmailIgnoreCase(assessoriaId, email)) {
                optoutRepo.save(new EmailOptout(assessoriaId, email, "unsubscribe_link"));
                log.info("email.unsubscribe assessoriaId={} email={}", assessoriaId, email);
            }
        } catch (Exception e) {
            log.warn("email.unsubscribe.invalid_token token={}", token);
        }
    }
}
