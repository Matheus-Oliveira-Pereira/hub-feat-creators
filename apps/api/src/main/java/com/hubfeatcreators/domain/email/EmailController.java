package com.hubfeatcreators.domain.email;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import com.hubfeatcreators.infra.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/email")
public class EmailController {

    private final EmailAccountService accountService;
    private final EmailTemplateService templateService;
    private final EmailLayoutRepository layoutRepo;
    private final EmailSendService sendService;
    private final EmailEventoRepository eventoRepo;

    public EmailController(
            EmailAccountService accountService,
            EmailTemplateService templateService,
            EmailLayoutRepository layoutRepo,
            EmailSendService sendService,
            EmailEventoRepository eventoRepo) {
        this.accountService = accountService;
        this.templateService = templateService;
        this.layoutRepo = layoutRepo;
        this.sendService = sendService;
        this.eventoRepo = eventoRepo;
    }

    // ─── Account DTOs ────────────────────────────────────────────────────────

    public record AccountRequest(
            @NotBlank String nome,
            @NotBlank String host,
            @NotNull Integer port,
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String fromAddress,
            @NotBlank String fromName,
            TlsMode tlsMode,
            Integer dailyQuota) {}

    public record AccountUpdateRequest(
            String nome,
            String host,
            Integer port,
            String username,
            String password,
            String fromAddress,
            String fromName,
            TlsMode tlsMode,
            Integer dailyQuota) {}

    public record AccountResponse(
            UUID id,
            String nome,
            String host,
            int port,
            String username,
            String fromAddress,
            String fromName,
            TlsMode tlsMode,
            int dailyQuota,
            EmailAccountStatus status,
            int falhasAuthCount,
            Instant updatedAt) {}

    // ─── Template DTOs ────────────────────────────────────────────────────────

    public record TemplateRequest(
            @NotBlank String nome,
            @NotBlank String assunto,
            @NotBlank String corpoHtml,
            String corpoTexto,
            String[] variaveis) {}

    public record TemplateUpdateRequest(
            String nome, String assunto, String corpoHtml, String corpoTexto, String[] variaveis) {}

    public record TemplateResponse(
            UUID id,
            String nome,
            String assunto,
            String corpoHtml,
            String corpoTexto,
            String[] variaveis,
            Instant createdAt,
            Instant updatedAt) {}

    public record PreviewRequest(Map<String, Object> vars) {}

    // ─── Layout DTOs ─────────────────────────────────────────────────────────

    public record LayoutRequest(String headerHtml, String footerHtml) {}

    public record LayoutResponse(
            UUID id, String headerHtml, String footerHtml, Instant updatedAt) {}

    // ─── Envio DTOs ───────────────────────────────────────────────────────────

    public record EnvioRequest(
            @NotNull UUID accountId,
            @NotNull UUID templateId,
            @NotBlank String destinatarioEmail,
            String destinatarioNome,
            Map<String, Object> vars,
            Map<String, Object> contexto,
            UUID idempotencyKey,
            boolean trackingEnabled) {}

    public record EnvioResponse(
            UUID id,
            UUID accountId,
            UUID templateId,
            String destinatarioEmail,
            String destinatarioNome,
            String assunto,
            EmailEnvioStatus status,
            int tentativas,
            String smtpMessageId,
            Instant enviadoEm,
            Instant createdAt) {}

    public record EventoResponse(
            UUID id, EmailEventoTipo tipo, Map<String, Object> payload, Instant createdAt) {}

    // ─── Accounts ─────────────────────────────────────────────────────────────

    @GetMapping("/accounts")
    @RequirePermission(PermissionCodes.B_EML)
    public List<AccountResponse> listarAccounts(@AuthenticationPrincipal AuthPrincipal principal) {
        return accountService.listar(principal).stream().map(this::toAccountResponse).toList();
    }

    @GetMapping("/accounts/{id}")
    @RequirePermission(PermissionCodes.B_EML)
    public AccountResponse buscarAccount(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        return toAccountResponse(accountService.buscar(principal, id));
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.C_EML)
    public AccountResponse criarAccount(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody AccountRequest req) {
        TlsMode tls = req.tlsMode() != null ? req.tlsMode() : TlsMode.STARTTLS;
        int quota = req.dailyQuota() != null ? req.dailyQuota() : 500;
        return toAccountResponse(
                accountService.criar(
                        principal,
                        req.nome(),
                        req.host(),
                        req.port(),
                        req.username(),
                        req.password(),
                        req.fromAddress(),
                        req.fromName(),
                        tls,
                        quota));
    }

    @PatchMapping("/accounts/{id}")
    @RequirePermission(PermissionCodes.E_EML)
    public AccountResponse atualizarAccount(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @RequestBody AccountUpdateRequest req) {
        return toAccountResponse(
                accountService.atualizar(
                        principal,
                        id,
                        req.nome(),
                        req.host(),
                        req.port(),
                        req.username(),
                        req.password(),
                        req.fromAddress(),
                        req.fromName(),
                        req.tlsMode(),
                        req.dailyQuota()));
    }

    @DeleteMapping("/accounts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.D_EML)
    public void deletarAccount(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        accountService.deletar(principal, id);
    }

    @PostMapping("/accounts/{id}/test")
    @RequirePermission(PermissionCodes.E_EML)
    public void testarAccount(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        accountService.testarConexao(principal, id);
    }

    // ─── Templates ────────────────────────────────────────────────────────────

    @GetMapping("/templates")
    @RequirePermission(PermissionCodes.B_EML)
    public List<TemplateResponse> listarTemplates(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return templateService.listar(principal).stream().map(this::toTemplateResponse).toList();
    }

    @GetMapping("/templates/{id}")
    @RequirePermission(PermissionCodes.B_EML)
    public TemplateResponse buscarTemplate(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        return toTemplateResponse(templateService.buscar(principal, id));
    }

    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.C_EML)
    public TemplateResponse criarTemplate(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody TemplateRequest req) {
        return toTemplateResponse(
                templateService.criar(
                        principal,
                        req.nome(),
                        req.assunto(),
                        req.corpoHtml(),
                        req.corpoTexto(),
                        req.variaveis()));
    }

    @PatchMapping("/templates/{id}")
    @RequirePermission(PermissionCodes.E_EML)
    public TemplateResponse atualizarTemplate(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @RequestBody TemplateUpdateRequest req) {
        return toTemplateResponse(
                templateService.atualizar(
                        principal,
                        id,
                        req.nome(),
                        req.assunto(),
                        req.corpoHtml(),
                        req.corpoTexto(),
                        req.variaveis()));
    }

    @DeleteMapping("/templates/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.D_EML)
    public void deletarTemplate(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        templateService.deletar(principal, id);
    }

    @PostMapping("/templates/{id}/preview")
    @RequirePermission(PermissionCodes.B_EML)
    public Map<String, String> previewTemplate(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @RequestBody PreviewRequest req) {
        EmailTemplate template = templateService.buscar(principal, id);
        Map<String, Object> vars = req.vars() != null ? req.vars() : Map.of();
        String html = templateService.preview(principal.assessoriaId(), template, vars);
        return Map.of("html", html);
    }

    // ─── Layout ───────────────────────────────────────────────────────────────

    @GetMapping("/layout")
    @RequirePermission(PermissionCodes.B_EML)
    public LayoutResponse buscarLayout(@AuthenticationPrincipal AuthPrincipal principal) {
        EmailLayout layout =
                layoutRepo
                        .findByAssessoriaId(principal.assessoriaId())
                        .orElse(new EmailLayout(principal.assessoriaId()));
        return toLayoutResponse(layout);
    }

    @PutMapping("/layout")
    @RequirePermission(PermissionCodes.E_EML)
    public LayoutResponse salvarLayout(
            @AuthenticationPrincipal AuthPrincipal principal, @RequestBody LayoutRequest req) {
        EmailLayout layout =
                layoutRepo
                        .findByAssessoriaId(principal.assessoriaId())
                        .orElse(new EmailLayout(principal.assessoriaId()));
        if (req.headerHtml() != null) layout.setHeaderHtml(req.headerHtml());
        if (req.footerHtml() != null) layout.setFooterHtml(req.footerHtml());
        layout.setUpdatedAt(Instant.now());
        return toLayoutResponse(layoutRepo.save(layout));
    }

    // ─── Envios ───────────────────────────────────────────────────────────────

    @PostMapping("/envios")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.C_EML)
    public EnvioResponse enviar(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody EnvioRequest req) {
        UUID idempotencyKey =
                req.idempotencyKey() != null ? req.idempotencyKey() : UUID.randomUUID();
        return toEnvioResponse(
                sendService.enviar(
                        principal,
                        req.accountId(),
                        req.templateId(),
                        req.destinatarioEmail(),
                        req.destinatarioNome(),
                        req.vars() != null ? req.vars() : Map.of(),
                        req.contexto() != null ? req.contexto() : Map.of(),
                        idempotencyKey,
                        req.trackingEnabled()));
    }

    @GetMapping("/envios")
    @RequirePermission(PermissionCodes.B_EML)
    public PageResponse<EnvioResponse> listarEnvios(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false) String contexto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<EmailEnvio> result =
                sendService.listar(
                        principal,
                        contexto,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()));
        List<EnvioResponse> data = result.getContent().stream().map(this::toEnvioResponse).toList();
        String nextCursor = result.hasNext() ? String.valueOf(page + 1) : null;
        return PageResponse.of(data, nextCursor, size);
    }

    @GetMapping("/envios/{id}")
    @RequirePermission(PermissionCodes.B_EML)
    public EnvioResponse buscarEnvio(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        return toEnvioResponse(sendService.buscar(principal, id));
    }

    @GetMapping("/envios/{id}/eventos")
    @RequirePermission(PermissionCodes.B_EML)
    public List<EventoResponse> eventosEnvio(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        sendService.buscar(principal, id); // ensure access
        return eventoRepo.findByEnvioIdOrderByCreatedAtDesc(id).stream()
                .map(
                        e ->
                                new EventoResponse(
                                        e.getId(), e.getTipo(), e.getPayload(), e.getCreatedAt()))
                .toList();
    }

    // ─── Mappers ──────────────────────────────────────────────────────────────

    private AccountResponse toAccountResponse(EmailAccount a) {
        return new AccountResponse(
                a.getId(),
                a.getNome(),
                a.getHost(),
                a.getPort(),
                a.getUsername(),
                a.getFromAddress(),
                a.getFromName(),
                a.getTlsMode(),
                a.getDailyQuota(),
                a.getStatus(),
                a.getFalhasAuthCount(),
                a.getUpdatedAt());
    }

    private TemplateResponse toTemplateResponse(EmailTemplate t) {
        return new TemplateResponse(
                t.getId(),
                t.getNome(),
                t.getAssunto(),
                t.getCorpoHtml(),
                t.getCorpoTexto(),
                t.getVariaveisDeclararadas(),
                t.getCreatedAt(),
                t.getUpdatedAt());
    }

    private LayoutResponse toLayoutResponse(EmailLayout l) {
        return new LayoutResponse(
                l.getId(), l.getHeaderHtml(), l.getFooterHtml(), l.getUpdatedAt());
    }

    private EnvioResponse toEnvioResponse(EmailEnvio e) {
        return new EnvioResponse(
                e.getId(),
                e.getAccountId(),
                e.getTemplateId(),
                e.getDestinatarioEmail(),
                e.getDestinatarioNome(),
                e.getAssunto(),
                e.getStatus(),
                e.getTentativas(),
                e.getSmtpMessageId(),
                e.getEnviadoEm(),
                e.getCreatedAt());
    }
}
