package com.hubfeatcreators.domain.email;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "email_envios")
public class EmailEnvio {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "destinatario_email", nullable = false)
    private String destinatarioEmail;

    @Column(name = "destinatario_nome")
    private String destinatarioNome;

    @Column(nullable = false)
    private String assunto;

    @Column(name = "corpo_html_renderizado", nullable = false)
    private String corpoHtmlRenderizado;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> contexto = Map.of();

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailEnvioStatus status = EmailEnvioStatus.ENFILEIRADO;

    @Column(name = "smtp_message_id")
    private String smtpMessageId;

    @Column(name = "enviado_em")
    private Instant enviadoEm;

    @Column(name = "falha_motivo")
    private String falhaMotivo;

    @Column(nullable = false)
    private int tentativas = 0;

    @Column(name = "tracking_enabled", nullable = false)
    private boolean trackingEnabled = false;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected EmailEnvio() {}

    public EmailEnvio(
            UUID assessoriaId,
            UUID accountId,
            UUID templateId,
            String destinatarioEmail,
            String destinatarioNome,
            String assunto,
            String corpoHtmlRenderizado,
            Map<String, Object> contexto,
            UUID idempotencyKey,
            boolean trackingEnabled,
            UUID autorId) {
        this.assessoriaId = assessoriaId;
        this.accountId = accountId;
        this.templateId = templateId;
        this.destinatarioEmail = destinatarioEmail;
        this.destinatarioNome = destinatarioNome;
        this.assunto = assunto;
        this.corpoHtmlRenderizado = corpoHtmlRenderizado;
        this.contexto = contexto != null ? contexto : Map.of();
        this.idempotencyKey = idempotencyKey;
        this.trackingEnabled = trackingEnabled;
        this.autorId = autorId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAssessoriaId() {
        return assessoriaId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public String getDestinatarioEmail() {
        return destinatarioEmail;
    }

    public String getDestinatarioNome() {
        return destinatarioNome;
    }

    public String getAssunto() {
        return assunto;
    }

    public String getCorpoHtmlRenderizado() {
        return corpoHtmlRenderizado;
    }

    public Map<String, Object> getContexto() {
        return contexto;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public EmailEnvioStatus getStatus() {
        return status;
    }

    public void setStatus(EmailEnvioStatus v) {
        this.status = v;
    }

    public String getSmtpMessageId() {
        return smtpMessageId;
    }

    public void setSmtpMessageId(String v) {
        this.smtpMessageId = v;
    }

    public Instant getEnviadoEm() {
        return enviadoEm;
    }

    public void setEnviadoEm(Instant v) {
        this.enviadoEm = v;
    }

    public String getFalhaMotivo() {
        return falhaMotivo;
    }

    public void setFalhaMotivo(String v) {
        this.falhaMotivo = v;
    }

    public int getTentativas() {
        return tentativas;
    }

    public void setTentativas(int v) {
        this.tentativas = v;
    }

    public boolean isTrackingEnabled() {
        return trackingEnabled;
    }

    public UUID getAutorId() {
        return autorId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
