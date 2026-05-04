package com.hubfeatcreators.domain.email;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "email_eventos")
public class EmailEvento {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "envio_id", nullable = false)
    private UUID envioId;

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailEventoTipo tipo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = Map.of();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected EmailEvento() {}

    public EmailEvento(
            UUID envioId, UUID assessoriaId, EmailEventoTipo tipo, Map<String, Object> payload) {
        this.envioId = envioId;
        this.assessoriaId = assessoriaId;
        this.tipo = tipo;
        this.payload = payload != null ? payload : Map.of();
    }

    public UUID getId() {
        return id;
    }

    public UUID getEnvioId() {
        return envioId;
    }

    public UUID getAssessoriaId() {
        return assessoriaId;
    }

    public EmailEventoTipo getTipo() {
        return tipo;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
