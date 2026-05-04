package com.hubfeatcreators.domain.whatsapp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubfeatcreators.infra.job.Job;
import com.hubfeatcreators.infra.job.JobRepository;
import com.hubfeatcreators.infra.web.BusinessException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsappService {

    private final WhatsappEnvioRepository envioRepo;
    private final WhatsappAccountService accountService;
    private final WhatsappTemplateRepository templateRepo;
    private final WhatsappOptoutRepository optoutRepo;
    private final WhatsappWindowCacheRepository windowRepo;
    private final WhatsappEventoInboundRepository inboundRepo;
    private final JobRepository jobRepo;
    private final MetaApiClient meta;
    private final ObjectMapper objectMapper;

    public WhatsappService(WhatsappEnvioRepository envioRepo,
            WhatsappAccountService accountService,
            WhatsappTemplateRepository templateRepo,
            WhatsappOptoutRepository optoutRepo,
            WhatsappWindowCacheRepository windowRepo,
            WhatsappEventoInboundRepository inboundRepo,
            JobRepository jobRepo,
            MetaApiClient meta,
            ObjectMapper objectMapper) {
        this.envioRepo = envioRepo;
        this.accountService = accountService;
        this.templateRepo = templateRepo;
        this.optoutRepo = optoutRepo;
        this.windowRepo = windowRepo;
        this.inboundRepo = inboundRepo;
        this.jobRepo = jobRepo;
        this.meta = meta;
        this.objectMapper = objectMapper;
    }

    /**
     * Enqueues a template message. Idempotent: same key returns existing envio.
     */
    @Transactional
    public WhatsappEnvio sendTemplate(UUID assessoriaId, UUID accountId, UUID templateId,
            String destinatarioE164, List<Map<String, Object>> components,
            UUID idempotencyKey, UUID autorId) {

        var existing = envioRepo.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) return existing.get();

        checkOptout(assessoriaId, destinatarioE164);
        accountService.requireAccount(assessoriaId, accountId);

        var template = templateRepo.findByIdAndAssessoriaId(templateId, assessoriaId)
                .orElseThrow(BusinessException::notFound);
        if (!"APPROVED".equals(template.getStatus())) {
            throw BusinessException.unprocessable("TEMPLATE_NAO_APROVADO",
                    "Template não aprovado pela Meta.");
        }

        String payload = toJson(Map.of("components", components,
                "templateNome", template.getNome(), "idioma", template.getIdioma()));
        var envio = new WhatsappEnvio(assessoriaId, accountId, templateId,
                destinatarioE164, "TEMPLATE", payload, idempotencyKey, autorId);
        envioRepo.save(envio);
        enqueueJob(envio);
        return envio;
    }

    /**
     * Enqueues a free-form message. Requires open 24h window.
     */
    @Transactional
    public WhatsappEnvio sendFreeform(UUID assessoriaId, UUID accountId,
            String destinatarioE164, String text, UUID idempotencyKey, UUID autorId) {

        var existing = envioRepo.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) return existing.get();

        checkOptout(assessoriaId, destinatarioE164);

        boolean windowOpen = windowRepo
                .findByIdAssessoriaIdAndIdE164(assessoriaId, destinatarioE164)
                .map(WhatsappWindowCache::isWindowOpen)
                .orElse(false);
        if (!windowOpen) {
            throw BusinessException.unprocessable("JANELA_FECHADA",
                    "Janela de 24h fechada. Use um template HSM aprovado.");
        }

        accountService.requireAccount(assessoriaId, accountId);
        String payload = toJson(Map.of("text", text));
        var envio = new WhatsappEnvio(assessoriaId, accountId, null,
                destinatarioE164, "FREEFORM", payload, idempotencyKey, autorId);
        envioRepo.save(envio);
        enqueueJob(envio);
        return envio;
    }

    /** Called by WhatsappSendJobHandler to perform the actual Meta API call. */
    @Transactional
    public void processEnvio(UUID envioId) {
        WhatsappEnvio envio = envioRepo.findById(envioId).orElseThrow(BusinessException::notFound);
        if (!"ENFILEIRADO".equals(envio.getStatus())) return;

        WhatsappAccount account = accountService.requireAccount(envio.getAssessoriaId(), envio.getAccountId());
        if (account.isRateLimited()) {
            throw BusinessException.tooManyRequests("RATE_LIMIT",
                    "Limite diário de mensagens atingido para este número.");
        }

        String token = accountService.decryptToken(account);
        try {
            String wamid = switch (envio.getTipo()) {
                case "TEMPLATE" -> sendTemplateViaMeta(envio, account, token);
                case "FREEFORM" -> sendFreeformViaMeta(envio, account, token);
                default -> throw new IllegalStateException("Tipo desconhecido: " + envio.getTipo());
            };
            account.incrementSent();
            envio.setWamid(wamid);
            envio.setStatus("ENVIADO");
            envio.setSentAt(Instant.now());
        } catch (MetaApiException e) {
            envio.setStatus("FALHOU");
            envio.setFailedAt(Instant.now());
            envio.setFalhaMotivo(e.getMessage());
        }
        envioRepo.save(envio);
    }

    /** Called by webhook to update delivery/read status. Idempotent. */
    @Transactional
    public void updateStatus(String wamid, String status) {
        envioRepo.findByWamid(wamid).ifPresent(envio -> {
            Instant now = Instant.now();
            switch (status) {
                case "delivered" -> { envio.setStatus("ENTREGUE"); envio.setDeliveredAt(now); }
                case "read" -> { envio.setStatus("LIDO"); envio.setReadAt(now); }
                case "failed" -> { envio.setStatus("FALHOU"); envio.setFailedAt(now); }
                default -> { /* ignore other statuses */ }
            }
            envioRepo.save(envio);
        });
    }

    /** Records inbound message, updates 24h window, handles STOP keywords. */
    @Transactional
    public void handleInbound(UUID assessoriaId, UUID accountId,
            String fromE164, String wamid, String tipo, String messagePayload) {
        if (inboundRepo.existsByWamid(wamid)) return;

        var evento = new WhatsappEventoInbound(assessoriaId, accountId, fromE164, wamid, tipo, messagePayload);
        evento.setProcessadoEm(Instant.now());
        inboundRepo.save(evento);

        var window = windowRepo.findByIdAssessoriaIdAndIdE164(assessoriaId, fromE164)
                .orElseGet(() -> new WhatsappWindowCache(assessoriaId, fromE164, Instant.now()));
        window.setLastInboundAt(Instant.now());
        windowRepo.save(window);

        if (isOptoutKeyword(messagePayload)) {
            if (!optoutRepo.existsByAssessoriaIdAndE164IgnoreCase(assessoriaId, fromE164)) {
                optoutRepo.save(new WhatsappOptout(assessoriaId, fromE164, "STOP keyword"));
            }
        }
    }

    private void checkOptout(UUID assessoriaId, String e164) {
        if (optoutRepo.existsByAssessoriaIdAndE164IgnoreCase(assessoriaId, e164)) {
            throw BusinessException.unprocessable("SEM_OPTIN",
                    "Contato optou por não receber mensagens WhatsApp.");
        }
    }

    private boolean isOptoutKeyword(String payload) {
        if (payload == null) return false;
        String lower = payload.toLowerCase();
        return lower.contains("parar") || lower.contains("sair") || lower.contains("stop");
    }

    private void enqueueJob(WhatsappEnvio envio) {
        var job = new Job(envio.getAssessoriaId(), "WHATSAPP_SEND",
                Map.of("envioId", envio.getId().toString()), null);
        jobRepo.save(job);
    }

    @SuppressWarnings("unchecked")
    private String sendTemplateViaMeta(WhatsappEnvio envio, WhatsappAccount account, String token) {
        try {
            Map<String, Object> p = objectMapper.readValue(envio.getPayload(), Map.class);
            List<Map<String, Object>> components = (List<Map<String, Object>>)
                    p.getOrDefault("components", Collections.emptyList());
            String templateNome = (String) p.getOrDefault("templateNome", "");
            String idioma = (String) p.getOrDefault("idioma", "pt_BR");
            return meta.sendTemplate(account.getPhoneNumberId(), token,
                    envio.getDestinatarioE164(), templateNome, idioma, components);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Payload inválido", e);
        }
    }

    @SuppressWarnings("unchecked")
    private String sendFreeformViaMeta(WhatsappEnvio envio, WhatsappAccount account, String token) {
        try {
            Map<String, Object> p = objectMapper.readValue(envio.getPayload(), Map.class);
            String text = (String) p.getOrDefault("text", "");
            return meta.sendText(account.getPhoneNumberId(), token, envio.getDestinatarioE164(), text);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Payload inválido", e);
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
