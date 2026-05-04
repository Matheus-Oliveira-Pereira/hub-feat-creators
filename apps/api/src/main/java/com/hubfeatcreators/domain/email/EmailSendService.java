package com.hubfeatcreators.domain.email;

import com.hubfeatcreators.infra.job.JobService;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.web.BusinessException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailSendService {

    private final EmailEnvioRepository envioRepo;
    private final EmailAccountRepository accountRepo;
    private final EmailTemplateService templateService;
    private final EmailOptoutRepository optoutRepo;
    private final JobService jobService;

    public EmailSendService(
            EmailEnvioRepository envioRepo,
            EmailAccountRepository accountRepo,
            EmailTemplateService templateService,
            EmailOptoutRepository optoutRepo,
            JobService jobService) {
        this.envioRepo = envioRepo;
        this.accountRepo = accountRepo;
        this.templateService = templateService;
        this.optoutRepo = optoutRepo;
        this.jobService = jobService;
    }

    /**
     * Enqueues an email for sending. Returns existing envio if idempotency_key already used.
     * Validates: account active, daily quota, opt-out.
     */
    @Transactional
    public EmailEnvio enviar(
            AuthPrincipal principal,
            UUID accountId,
            UUID templateId,
            String destinatarioEmail,
            String destinatarioNome,
            Map<String, Object> vars,
            Map<String, Object> contexto,
            UUID idempotencyKey,
            boolean trackingEnabled) {

        Optional<EmailEnvio> existing =
                envioRepo.findByAssessoriaIdAndIdempotencyKey(
                        principal.assessoriaId(), idempotencyKey);
        if (existing.isPresent()) return existing.get();

        EmailAccount account =
                accountRepo
                        .findByIdAndAssessoriaId(accountId, principal.assessoriaId())
                        .orElseThrow(() -> BusinessException.notFound("EMAIL_ACCOUNT"));

        if (account.getStatus() != EmailAccountStatus.ATIVA) {
            throw BusinessException.unprocessable(
                    "EMAIL_ACCOUNT_INACTIVE", "Conta de e-mail inativa");
        }

        Instant inicioDia =
                Instant.now()
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                        .atStartOfDay(ZoneOffset.UTC)
                        .toInstant();
        long enviosDia = accountRepo.countEnviosDia(accountId, inicioDia);
        if (enviosDia >= account.getDailyQuota()) {
            throw BusinessException.unprocessable(
                    "EMAIL_QUOTA_EXCEEDED", "Cota diária de envios atingida");
        }

        if (optoutRepo.existsByAssessoriaIdAndEmailIgnoreCase(
                principal.assessoriaId(), destinatarioEmail)) {
            throw BusinessException.unprocessable("EMAIL_OPTOUT", "Destinatário descadastrado");
        }

        EmailTemplate template = templateService.buscar(principal, templateId);
        String unsubscribeUrl = buildUnsubscribeUrl(principal.assessoriaId(), destinatarioEmail);
        String corpoRendered =
                templateService.renderizar(
                        principal.assessoriaId(), template, vars, unsubscribeUrl);

        EmailEnvio envio =
                new EmailEnvio(
                        principal.assessoriaId(),
                        accountId,
                        templateId,
                        destinatarioEmail,
                        destinatarioNome,
                        template.getAssunto(),
                        corpoRendered,
                        contexto,
                        idempotencyKey,
                        trackingEnabled,
                        principal.usuarioId());
        envio = envioRepo.save(envio);

        UUID envioId = envio.getId();
        jobService.enqueue(
                principal.assessoriaId(),
                "EMAIL_SEND",
                Map.of("envioId", envioId.toString()),
                idempotencyKey);

        return envio;
    }

    @Transactional(readOnly = true)
    public Page<EmailEnvio> listar(AuthPrincipal principal, String contextoKey, Pageable pageable) {
        return envioRepo.findByAssessoriaId(principal.assessoriaId(), contextoKey, pageable);
    }

    @Transactional(readOnly = true)
    public EmailEnvio buscar(AuthPrincipal principal, UUID id) {
        return envioRepo
                .findByIdAndAssessoriaId(id, principal.assessoriaId())
                .orElseThrow(() -> BusinessException.notFound("EMAIL_ENVIO"));
    }

    private String buildUnsubscribeUrl(UUID assessoriaId, String email) {
        String token =
                java.util.Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                (assessoriaId + ":" + email)
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return "/api/v1/email/unsubscribe?token=" + token;
    }
}
