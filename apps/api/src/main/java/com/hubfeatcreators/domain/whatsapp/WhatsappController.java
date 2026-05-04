package com.hubfeatcreators.domain.whatsapp;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/whatsapp")
public class WhatsappController {

    private final WhatsappAccountService accountService;
    private final WhatsappTemplateService templateService;
    private final WhatsappService whatsappService;

    public WhatsappController(WhatsappAccountService accountService,
            WhatsappTemplateService templateService,
            WhatsappService whatsappService) {
        this.accountService = accountService;
        this.templateService = templateService;
        this.whatsappService = whatsappService;
    }

    // ─── Accounts ──────────────────────────────────────────────────────────────

    public record AccountRequest(
            @NotBlank String wabaId,
            @NotBlank String phoneNumberId,
            @NotBlank String phoneE164,
            @NotBlank String displayName,
            @NotBlank String accessToken,
            @NotBlank String appSecret) {}

    public record AccountUpdateRequest(String displayName, String accessToken, String appSecret) {}

    public record AccountResponse(UUID id, String wabaId, String phoneNumberId, String phoneE164,
            String displayName, String status, int dailyLimit, int dailySent) {}

    private AccountResponse toAccountResp(WhatsappAccount a) {
        return new AccountResponse(a.getId(), a.getWabaId(), a.getPhoneNumberId(),
                a.getPhoneE164(), a.getDisplayName(), a.getStatus(),
                a.getDailyLimit(), a.getDailySent());
    }

    @RequirePermission(PermissionCodes.B_WAP)
    @GetMapping("/accounts")
    public List<AccountResponse> listAccounts(@AuthenticationPrincipal AuthPrincipal p) {
        return accountService.list(p.assessoriaId()).stream().map(this::toAccountResp).toList();
    }

    @RequirePermission(PermissionCodes.C_WAP)
    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@AuthenticationPrincipal AuthPrincipal p,
            @RequestBody @Valid AccountRequest req) {
        return toAccountResp(accountService.create(p.assessoriaId(), req.wabaId(),
                req.phoneNumberId(), req.phoneE164(), req.displayName(),
                req.accessToken(), req.appSecret()));
    }

    @RequirePermission(PermissionCodes.E_WAP)
    @PatchMapping("/accounts/{id}")
    public AccountResponse updateAccount(@AuthenticationPrincipal AuthPrincipal p,
            @PathVariable UUID id, @RequestBody AccountUpdateRequest req) {
        return toAccountResp(accountService.update(p.assessoriaId(), id,
                req.displayName(), req.accessToken(), req.appSecret()));
    }

    @RequirePermission(PermissionCodes.D_WAP)
    @DeleteMapping("/accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@AuthenticationPrincipal AuthPrincipal p, @PathVariable UUID id) {
        accountService.delete(p.assessoriaId(), id);
    }

    // ─── Templates ──────────────────────────────────────────────────────────────

    public record TemplateRequest(
            @NotNull UUID accountId,
            @NotBlank String nome,
            String idioma,
            @NotBlank String categoria,
            @NotBlank String corpo,
            String[] variaveis) {}

    public record TemplateResponse(UUID id, UUID accountId, String nome, String idioma,
            String categoria, String corpo, String[] variaveis, String status,
            String metaTemplateId, String motivoRejeicao) {}

    private TemplateResponse toTemplateResp(WhatsappTemplate t) {
        return new TemplateResponse(t.getId(), t.getAccountId(), t.getNome(), t.getIdioma(),
                t.getCategoria(), t.getCorpo(), t.getVariaveis(), t.getStatus(),
                t.getMetaTemplateId(), t.getMotivoRejeicao());
    }

    @RequirePermission(PermissionCodes.B_WAP)
    @GetMapping("/templates")
    public List<TemplateResponse> listTemplates(@AuthenticationPrincipal AuthPrincipal p) {
        return templateService.list(p.assessoriaId()).stream().map(this::toTemplateResp).toList();
    }

    @RequirePermission(PermissionCodes.C_WAP)
    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateResponse createTemplate(@AuthenticationPrincipal AuthPrincipal p,
            @RequestBody @Valid TemplateRequest req) {
        return toTemplateResp(templateService.create(p.assessoriaId(), req.accountId(),
                req.nome(), req.idioma() != null ? req.idioma() : "pt_BR",
                req.categoria(), req.corpo(), req.variaveis()));
    }

    @RequirePermission(PermissionCodes.C_WAP)
    @PostMapping("/templates/{id}/submit")
    public TemplateResponse submitTemplate(@AuthenticationPrincipal AuthPrincipal p,
            @PathVariable UUID id) {
        return toTemplateResp(templateService.submit(p.assessoriaId(), id));
    }

    // ─── Envios ─────────────────────────────────────────────────────────────────

    public record SendTemplateRequest(
            @NotNull UUID accountId,
            @NotNull UUID templateId,
            @NotBlank String destinatarioE164,
            List<Map<String, Object>> components,
            UUID idempotencyKey) {}

    public record SendFreeformRequest(
            @NotNull UUID accountId,
            @NotBlank String destinatarioE164,
            @NotBlank String text,
            UUID idempotencyKey) {}

    public record EnvioResponse(UUID id, String tipo, String destinatarioE164, String status,
            String wamid) {}

    private EnvioResponse toEnvioResp(WhatsappEnvio e) {
        return new EnvioResponse(e.getId(), e.getTipo(), e.getDestinatarioE164(),
                e.getStatus(), e.getWamid());
    }

    @RequirePermission(PermissionCodes.C_WAP)
    @PostMapping("/envios/template")
    @ResponseStatus(HttpStatus.CREATED)
    public EnvioResponse sendTemplate(@AuthenticationPrincipal AuthPrincipal p,
            @RequestBody @Valid SendTemplateRequest req) {
        UUID key = req.idempotencyKey() != null ? req.idempotencyKey() : UUID.randomUUID();
        return toEnvioResp(whatsappService.sendTemplate(p.assessoriaId(), req.accountId(),
                req.templateId(), req.destinatarioE164(),
                req.components() != null ? req.components() : Collections.emptyList(),
                key, p.usuarioId()));
    }

    @RequirePermission(PermissionCodes.C_WAP)
    @PostMapping("/envios/freeform")
    @ResponseStatus(HttpStatus.CREATED)
    public EnvioResponse sendFreeform(@AuthenticationPrincipal AuthPrincipal p,
            @RequestBody @Valid SendFreeformRequest req) {
        UUID key = req.idempotencyKey() != null ? req.idempotencyKey() : UUID.randomUUID();
        return toEnvioResp(whatsappService.sendFreeform(p.assessoriaId(), req.accountId(),
                req.destinatarioE164(), req.text(), key, p.usuarioId()));
    }
}
