package com.hubfeatcreators.domain.whatsapp;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "whatsapp_eventos_inbound")
public class WhatsappEventoInbound {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "from_e164", nullable = false)
    private String fromE164;

    @Column(nullable = false)
    private String wamid;

    @Column(nullable = false)
    private String tipo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload = "{}";

    @Column(name = "processado_em")
    private Instant processadoEm;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected WhatsappEventoInbound() {}

    public WhatsappEventoInbound(UUID assessoriaId, UUID accountId, String fromE164,
            String wamid, String tipo, String payload) {
        this.assessoriaId = assessoriaId;
        this.accountId = accountId;
        this.fromE164 = fromE164;
        this.wamid = wamid;
        this.tipo = tipo;
        this.payload = payload != null ? payload : "{}";
    }

    public UUID getId() { return id; }
    public UUID getAssessoriaId() { return assessoriaId; }
    public UUID getAccountId() { return accountId; }
    public String getFromE164() { return fromE164; }
    public String getWamid() { return wamid; }
    public String getTipo() { return tipo; }
    public String getPayload() { return payload; }
    public Instant getProcessadoEm() { return processadoEm; }
    public Instant getCreatedAt() { return createdAt; }
    public void setProcessadoEm(Instant v) { this.processadoEm = v; }
}
