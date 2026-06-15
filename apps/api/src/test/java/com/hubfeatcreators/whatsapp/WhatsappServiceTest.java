package com.hubfeatcreators.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubfeatcreators.domain.whatsapp.*;
import com.hubfeatcreators.infra.job.Job;
import com.hubfeatcreators.infra.job.JobRepository;
import com.hubfeatcreators.infra.web.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class WhatsappServiceTest {

    @Mock WhatsappEnvioRepository envioRepo;
    @Mock WhatsappAccountService accountService;
    @Mock WhatsappTemplateRepository templateRepo;
    @Mock WhatsappOptoutRepository optoutRepo;
    @Mock WhatsappWindowCacheRepository windowRepo;
    @Mock WhatsappEventoInboundRepository inboundRepo;
    @Mock JobRepository jobRepo;
    @Mock MetaApiClient meta;

    @InjectMocks WhatsappService service;

    final ObjectMapper objectMapper = new ObjectMapper();

    UUID accountId = UUID.randomUUID();
    UUID templateId = UUID.randomUUID();
    UUID autorId = UUID.randomUUID();
    UUID idempotencyKey = UUID.randomUUID();
    String phone = "+5511999990000";

    WhatsappAccount account;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Re-inject objectMapper manually (not picked up by @InjectMocks from field above)
        service =
                new WhatsappService(
                        envioRepo,
                        accountService,
                        templateRepo,
                        optoutRepo,
                        windowRepo,
                        inboundRepo,
                        jobRepo,
                        meta,
                        objectMapper);

        account =
                new WhatsappAccount(
                        "waba-1",
                        "phone-id-1",
                        phone,
                        "Test",
                        new byte[16],
                        new byte[12],
                        new byte[16],
                        new byte[12]);

        when(envioRepo.save(any()))
                .thenAnswer(
                        inv -> {
                            WhatsappEnvio e = inv.getArgument(0);
                            if (e.getId() == null) {
                                var f = WhatsappEnvio.class.getDeclaredField("id");
                                f.setAccessible(true);
                                f.set(e, UUID.randomUUID());
                            }
                            return e;
                        });
        when(jobRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(accountService.requireAccount(any())).thenReturn(account);
        when(optoutRepo.existsByE164IgnoreCase(any())).thenReturn(false);
    }

    // ── sendTemplate ──────────────────────────────────────────────────────────

    @Test
    void sendTemplate_idempotent_returns_existing() {
        WhatsappEnvio existing = buildEnvio("TEMPLATE");
        when(envioRepo.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existing));

        WhatsappEnvio result =
                service.sendTemplate(
                        accountId, templateId, phone, List.of(), idempotencyKey, autorId);

        assertThat(result).isSameAs(existing);
        verify(envioRepo, never()).save(any());
        verify(jobRepo, never()).save(any());
    }

    @Test
    void sendTemplate_throws_when_opted_out() {
        when(envioRepo.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(optoutRepo.existsByE164IgnoreCase(phone)).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                service.sendTemplate(
                                        accountId,
                                        templateId,
                                        phone,
                                        List.of(),
                                        idempotencyKey,
                                        autorId))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> assertThat(((BusinessException) e).getCode()).isEqualTo("SEM_OPTIN"));
    }

    @Test
    void sendTemplate_throws_when_template_not_approved() {
        WhatsappTemplate tmpl =
                new WhatsappTemplate(accountId, "welcome", "pt_BR", "MARKETING", "body", null);
        // status default = PENDING
        when(envioRepo.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(templateRepo.findById(templateId)).thenReturn(Optional.of(tmpl));

        assertThatThrownBy(
                        () ->
                                service.sendTemplate(
                                        accountId,
                                        templateId,
                                        phone,
                                        List.of(),
                                        idempotencyKey,
                                        autorId))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e ->
                                assertThat(((BusinessException) e).getCode())
                                        .isEqualTo("TEMPLATE_NAO_APROVADO"));
    }

    @Test
    void sendTemplate_enqueues_job_for_approved_template() {
        WhatsappTemplate tmpl =
                new WhatsappTemplate(accountId, "welcome", "pt_BR", "MARKETING", "body", null);
        tmpl.setStatus("APPROVED");
        when(envioRepo.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(templateRepo.findById(templateId)).thenReturn(Optional.of(tmpl));

        service.sendTemplate(accountId, templateId, phone, List.of(), idempotencyKey, autorId);

        verify(envioRepo).save(any(WhatsappEnvio.class));
        ArgumentCaptor<Job> jobCaptor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepo).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getTipo()).isEqualTo("WHATSAPP_SEND");
    }

    // ── sendFreeform ──────────────────────────────────────────────────────────

    @Test
    void sendFreeform_idempotent_returns_existing() {
        WhatsappEnvio existing = buildEnvio("FREEFORM");
        when(envioRepo.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existing));

        WhatsappEnvio result =
                service.sendFreeform(accountId, phone, "Olá", idempotencyKey, autorId);

        assertThat(result).isSameAs(existing);
        verify(envioRepo, never()).save(any());
    }

    @Test
    void sendFreeform_throws_when_opted_out() {
        when(envioRepo.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(optoutRepo.existsByE164IgnoreCase(phone)).thenReturn(true);

        assertThatThrownBy(
                        () ->
                                service.sendFreeform(
                                        accountId, phone, "Olá", idempotencyKey, autorId))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> assertThat(((BusinessException) e).getCode()).isEqualTo("SEM_OPTIN"));
    }

    @Test
    void sendFreeform_throws_when_window_closed() {
        when(envioRepo.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        when(windowRepo.findByE164(phone)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                service.sendFreeform(
                                        accountId, phone, "Olá", idempotencyKey, autorId))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e ->
                                assertThat(((BusinessException) e).getCode())
                                        .isEqualTo("JANELA_FECHADA"));
    }

    @Test
    void sendFreeform_throws_when_window_expired() {
        when(envioRepo.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        WhatsappWindowCache expiredWindow =
                new WhatsappWindowCache(phone, Instant.now().minusSeconds(86401));
        when(windowRepo.findByE164(phone)).thenReturn(Optional.of(expiredWindow));

        assertThatThrownBy(
                        () ->
                                service.sendFreeform(
                                        accountId, phone, "Olá", idempotencyKey, autorId))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e ->
                                assertThat(((BusinessException) e).getCode())
                                        .isEqualTo("JANELA_FECHADA"));
    }

    @Test
    void sendFreeform_enqueues_job_when_window_open() {
        when(envioRepo.findByIdempotencyKey(any())).thenReturn(Optional.empty());
        WhatsappWindowCache openWindow = new WhatsappWindowCache(phone, Instant.now());
        when(windowRepo.findByE164(phone)).thenReturn(Optional.of(openWindow));

        service.sendFreeform(accountId, phone, "Olá", idempotencyKey, autorId);

        verify(envioRepo).save(any(WhatsappEnvio.class));
        verify(jobRepo).save(any(Job.class));
    }

    // ── processEnvio ─────────────────────────────────────────────────────────

    @Test
    void processEnvio_skips_non_enfileirado() {
        WhatsappEnvio envio = buildEnvio("TEMPLATE");
        envio.setStatus("ENVIADO");
        when(envioRepo.findById(envio.getId())).thenReturn(Optional.of(envio));

        service.processEnvio(envio.getId());

        verify(meta, never()).sendTemplate(any(), any(), any(), any(), any(), any());
        verify(envioRepo, never()).save(any());
    }

    @Test
    void processEnvio_marks_enviado_on_success() throws Exception {
        WhatsappEnvio envio =
                buildEnvioWithPayload(
                        "TEMPLATE",
                        "{\"templateNome\":\"welcome\",\"idioma\":\"pt_BR\",\"components\":[]}");
        when(envioRepo.findById(envio.getId())).thenReturn(Optional.of(envio));
        when(accountService.decryptToken(account)).thenReturn("token-abc");
        when(meta.sendTemplate(any(), any(), any(), any(), any(), any())).thenReturn("wamid-001");

        service.processEnvio(envio.getId());

        assertThat(envio.getStatus()).isEqualTo("ENVIADO");
        assertThat(envio.getWamid()).isEqualTo("wamid-001");
        verify(envioRepo).save(envio);
    }

    @Test
    void processEnvio_marks_falhou_on_meta_error() throws Exception {
        WhatsappEnvio envio =
                buildEnvioWithPayload(
                        "TEMPLATE",
                        "{\"templateNome\":\"welcome\",\"idioma\":\"pt_BR\",\"components\":[]}");
        when(envioRepo.findById(envio.getId())).thenReturn(Optional.of(envio));
        when(accountService.decryptToken(account)).thenReturn("bad-token");
        when(meta.sendTemplate(any(), any(), any(), any(), any(), any()))
                .thenThrow(new MetaApiException("Invalid token"));

        service.processEnvio(envio.getId());

        assertThat(envio.getStatus()).isEqualTo("FALHOU");
        verify(envioRepo).save(envio);
    }

    @Test
    void processEnvio_throws_when_rate_limited() {
        // exhaust the daily limit
        for (int i = 0; i < 1001; i++) account.incrementSent();

        WhatsappEnvio envio = buildEnvioWithPayload("TEMPLATE", "{}");
        when(envioRepo.findById(envio.getId())).thenReturn(Optional.of(envio));

        assertThatThrownBy(() -> service.processEnvio(envio.getId()))
                .isInstanceOf(BusinessException.class)
                .satisfies(
                        e -> assertThat(((BusinessException) e).getCode()).isEqualTo("RATE_LIMIT"));
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test
    void updateStatus_sets_entregue() {
        WhatsappEnvio envio = buildEnvio("TEMPLATE");
        when(envioRepo.findByWamid("wamid-x")).thenReturn(Optional.of(envio));

        service.updateStatus("wamid-x", "delivered");

        assertThat(envio.getStatus()).isEqualTo("ENTREGUE");
        verify(envioRepo).save(envio);
    }

    @Test
    void updateStatus_sets_lido() {
        WhatsappEnvio envio = buildEnvio("TEMPLATE");
        when(envioRepo.findByWamid("wamid-x")).thenReturn(Optional.of(envio));

        service.updateStatus("wamid-x", "read");

        assertThat(envio.getStatus()).isEqualTo("LIDO");
        verify(envioRepo).save(envio);
    }

    @Test
    void updateStatus_sets_falhou() {
        WhatsappEnvio envio = buildEnvio("TEMPLATE");
        when(envioRepo.findByWamid("wamid-x")).thenReturn(Optional.of(envio));

        service.updateStatus("wamid-x", "failed");

        assertThat(envio.getStatus()).isEqualTo("FALHOU");
    }

    @Test
    void updateStatus_noop_for_unknown_wamid() {
        when(envioRepo.findByWamid(any())).thenReturn(Optional.empty());

        service.updateStatus("unknown-id", "delivered");

        verify(envioRepo, never()).save(any());
    }

    // ── handleInbound ─────────────────────────────────────────────────────────

    @Test
    void handleInbound_idempotent_when_wamid_exists() {
        when(inboundRepo.existsByWamid("wamid-dup")).thenReturn(true);

        service.handleInbound(accountId, phone, "wamid-dup", "TEXT", "Oi");

        verify(inboundRepo, never()).save(any());
        verify(windowRepo, never()).save(any());
    }

    @Test
    void handleInbound_saves_evento_and_updates_window() {
        when(inboundRepo.existsByWamid("wamid-new")).thenReturn(false);
        when(windowRepo.findByE164(phone)).thenReturn(Optional.empty());
        when(windowRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inboundRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handleInbound(accountId, phone, "wamid-new", "TEXT", "Olá");

        verify(inboundRepo).save(any(WhatsappEventoInbound.class));
        verify(windowRepo).save(any(WhatsappWindowCache.class));
    }

    @Test
    void handleInbound_registers_optout_on_stop_keyword() {
        when(inboundRepo.existsByWamid(any())).thenReturn(false);
        when(windowRepo.findByE164(any())).thenReturn(Optional.empty());
        when(windowRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inboundRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(optoutRepo.existsByE164IgnoreCase(phone)).thenReturn(false);
        when(optoutRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handleInbound(accountId, phone, "wamid-stop", "TEXT", "STOP");

        verify(optoutRepo).save(any(WhatsappOptout.class));
    }

    @Test
    void handleInbound_registers_optout_on_parar_keyword() {
        when(inboundRepo.existsByWamid(any())).thenReturn(false);
        when(windowRepo.findByE164(any())).thenReturn(Optional.empty());
        when(windowRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inboundRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(optoutRepo.existsByE164IgnoreCase(phone)).thenReturn(false);
        when(optoutRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handleInbound(accountId, phone, "wamid-parar", "TEXT", "quero parar de receber");

        verify(optoutRepo).save(any(WhatsappOptout.class));
    }

    @Test
    void handleInbound_no_optout_for_normal_message() {
        when(inboundRepo.existsByWamid(any())).thenReturn(false);
        when(windowRepo.findByE164(any())).thenReturn(Optional.empty());
        when(windowRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inboundRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handleInbound(accountId, phone, "wamid-ok", "TEXT", "Tudo bem?");

        verify(optoutRepo, never()).save(any());
    }

    @Test
    void handleInbound_optout_idempotent_when_already_opted_out() {
        when(inboundRepo.existsByWamid(any())).thenReturn(false);
        when(windowRepo.findByE164(any())).thenReturn(Optional.empty());
        when(windowRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(inboundRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(optoutRepo.existsByE164IgnoreCase(phone)).thenReturn(true);

        service.handleInbound(accountId, phone, "wamid-stop2", "TEXT", "stop");

        verify(optoutRepo, never()).save(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private WhatsappEnvio buildEnvio(String tipo) {
        return buildEnvioWithPayload(tipo, "{}");
    }

    private WhatsappEnvio buildEnvioWithPayload(String tipo, String payload) {
        return new WhatsappEnvio(
                accountId, templateId, phone, tipo, payload, idempotencyKey, autorId);
    }
}
