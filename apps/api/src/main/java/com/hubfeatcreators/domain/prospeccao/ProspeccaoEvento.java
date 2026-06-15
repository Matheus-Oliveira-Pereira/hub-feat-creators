package com.hubfeatcreators.domain.prospeccao;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "prospeccao_eventos")
public class ProspeccaoEvento {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "prospeccao_id", nullable = false)
    private UUID prospeccaoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventoTipo tipo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = new HashMap<>();

    @Column(name = "autor_id")
    private UUID autorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ProspeccaoEvento() {}

    public ProspeccaoEvento(
            UUID prospeccaoId, EventoTipo tipo, Map<String, Object> payload, UUID autorId) {
        this.prospeccaoId = prospeccaoId;
        this.tipo = tipo;
        this.payload = payload != null ? payload : new HashMap<>();
        this.autorId = autorId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProspeccaoId() {
        return prospeccaoId;
    }

    public EventoTipo getTipo() {
        return tipo;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public UUID getAutorId() {
        return autorId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
