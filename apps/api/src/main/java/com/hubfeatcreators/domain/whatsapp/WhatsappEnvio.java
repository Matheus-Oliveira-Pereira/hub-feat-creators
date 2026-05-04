package com.hubfeatcreators.domain.whatsapp;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "whatsapp_envios")
public class WhatsappEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "template_id")
    private UUID templateId;

    @Column(name = "destinatario_e164", nullable = false)
    private String destinatarioE164;

    @Column(nullable = false)
    private String tipo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload = "{}";

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;

    @Column(nullable = false)
    private String status = "ENFILEIRADO";

    private String wamid;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String contexto;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "failed_at")
    private Instant failedAt;

    @Column(name = "falha_motivo")
    private String falhaMotivo;

    protected WhatsappEnvio() {}

    public WhatsappEnvio(UUID assessoriaId, UUID accountId, UUID templateId,
            String destinatarioE164, String tipo, String payload,
            UUID idempotencyKey, UUID autorId) {
        this.assessoriaId = assessoriaId;
        this.accountId = accountId;
        this.templateId = templateId;
        this.destinatarioE164 = destinatarioE164;
        this.tipo = tipo;
        this.payload = payload != null ? payload : "{}";
        this.idempotencyKey = idempotencyKey;
        this.autorId = autorId;
    }

    public UUID getId() { return id; }
    public UUID getAssessoriaId() { return assessoriaId; }
    public UUID getAccountId() { return accountId; }
    public UUID getTemplateId() { return templateId; }
    public String getDestinatarioE164() { return destinatarioE164; }
    public String getTipo() { return tipo; }
    public String getPayload() { return payload; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public String getStatus() { return status; }
    public String getWamid() { return wamid; }
    public String getContexto() { return contexto; }
    public UUID getAutorId() { return autorId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSentAt() { return sentAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public Instant getReadAt() { return readAt; }
    public Instant getFailedAt() { return failedAt; }
    public String getFalhaMotivo() { return falhaMotivo; }
    public void setStatus(String v) { this.status = v; }
    public void setWamid(String v) { this.wamid = v; }
    public void setSentAt(Instant v) { this.sentAt = v; }
    public void setDeliveredAt(Instant v) { this.deliveredAt = v; }
    public void setReadAt(Instant v) { this.readAt = v; }
    public void setFailedAt(Instant v) { this.failedAt = v; }
    public void setFalhaMotivo(String v) { this.falhaMotivo = v; }
}
