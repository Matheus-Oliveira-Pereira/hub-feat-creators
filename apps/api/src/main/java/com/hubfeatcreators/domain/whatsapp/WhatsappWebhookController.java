package com.hubfeatcreators.domain.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public webhook endpoint for Meta WhatsApp Cloud API.
 * Signature validated with X-Hub-Signature-256 (HMAC SHA256).
 * Both GET (challenge verification) and POST (events) are handled here.
 */
@RestController
@RequestMapping("/api/v1/whatsapp/webhook")
public class WhatsappWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsappWebhookController.class);

    private final WhatsappAccountRepository accountRepo;
    private final WhatsappAccountService accountService;
    private final WhatsappService whatsappService;
    private final ObjectMapper objectMapper;

    public WhatsappWebhookController(WhatsappAccountRepository accountRepo,
            WhatsappAccountService accountService,
            WhatsappService whatsappService,
            ObjectMapper objectMapper) {
        this.accountRepo = accountRepo;
        this.accountService = accountService;
        this.whatsappService = whatsappService;
        this.objectMapper = objectMapper;
    }

    /** Meta webhook verification (GET) */
    @GetMapping
    public ResponseEntity<String> verify(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String verifyToken) {
        if ("subscribe".equals(mode)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    /** Meta webhook events (POST) */
    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature) {

        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            log.warn("webhook.parse.failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        JsonNode entries = root.path("entry");
        if (!entries.isArray()) return ResponseEntity.ok().build();

        for (JsonNode entry : entries) {
            JsonNode changes = entry.path("changes");
            for (JsonNode change : changes) {
                JsonNode value = change.path("value");
                String phoneNumberId = value.path("metadata").path("phone_number_id").asText(null);
                if (phoneNumberId == null) continue;

                // Find account by phoneNumberId to validate signature
                var accountOpt = accountRepo.findAll().stream()
                        .filter(a -> a.getPhoneNumberId().equals(phoneNumberId) && a.isAtivo())
                        .findFirst();
                if (accountOpt.isEmpty()) continue;

                var account = accountOpt.get();

                if (signature != null && !validateSignature(rawBody, signature, account)) {
                    log.warn("webhook.signature.invalid phoneNumberId={}", phoneNumberId);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }

                // Process status updates
                JsonNode statuses = value.path("statuses");
                if (statuses.isArray()) {
                    for (JsonNode s : statuses) {
                        String wamid = s.path("id").asText(null);
                        String status = s.path("status").asText(null);
                        if (wamid != null && status != null) {
                            whatsappService.updateStatus(wamid, status);
                        }
                    }
                }

                // Process inbound messages
                JsonNode messages = value.path("messages");
                if (messages.isArray()) {
                    for (JsonNode msg : messages) {
                        String wamid = msg.path("id").asText(null);
                        String from = msg.path("from").asText(null);
                        String tipo = msg.path("type").asText("TEXT").toUpperCase();
                        if (wamid == null || from == null) continue;
                        String msgPayload = msg.toString();
                        try {
                            whatsappService.handleInbound(account.getAssessoriaId(),
                                    account.getId(), from, wamid, tipo, msgPayload);
                        } catch (Exception e) {
                            log.warn("webhook.inbound.failed wamid={}: {}", wamid, e.getMessage());
                        }
                    }
                }
            }
        }

        return ResponseEntity.ok().build();
    }

    private boolean validateSignature(String body, String signatureHeader, WhatsappAccount account) {
        try {
            if (!signatureHeader.startsWith("sha256=")) return false;
            String expected = signatureHeader.substring(7);
            String appSecret = accountService.decryptAppSecret(account);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
            String computed = bytesToHex(hash);
            return computed.equals(expected);
        } catch (Exception e) {
            log.warn("webhook.signature.error: {}", e.getMessage());
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
