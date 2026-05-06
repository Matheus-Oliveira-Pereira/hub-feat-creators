package com.hubfeatcreators.domain.historico;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "eventos")
public class Evento {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(nullable = false)
    private String tipo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entidades_relacionadas", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, String>> entidadesRelacionadas = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = new HashMap<>();

    @Column(name = "autor_id")
    private UUID autorId;

    @Column(nullable = false)
    private Instant ts = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Evento() {}

    private Evento(
            UUID assessoriaId,
            String tipo,
            UUID autorId,
            Map<String, Object> payload,
            List<Map<String, String>> entidades) {
        this.assessoriaId = assessoriaId;
        this.tipo = tipo;
        this.autorId = autorId;
        this.payload = payload != null ? payload : new HashMap<>();
        this.entidadesRelacionadas = entidades != null ? entidades : new ArrayList<>();
    }

    public static Evento of(
            UUID assessoriaId,
            EventoTipo tipo,
            UUID autorId,
            Map<String, Object> payload,
            EntidadeRef... entidades) {
        List<Map<String, String>> refs = new ArrayList<>();
        for (EntidadeRef ref : entidades) {
            Map<String, String> m = new HashMap<>();
            m.put("tipo", ref.tipo());
            m.put("id", ref.id().toString());
            refs.add(m);
        }
        return new Evento(assessoriaId, tipo.name(), autorId, payload, refs);
    }

    public record EntidadeRef(String tipo, UUID id) {}

    public UUID getId() {
        return id;
    }

    public UUID getAssessoriaId() {
        return assessoriaId;
    }

    public String getTipo() {
        return tipo;
    }

    public List<Map<String, String>> getEntidadesRelacionadas() {
        return entidadesRelacionadas;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public UUID getAutorId() {
        return autorId;
    }

    public Instant getTs() {
        return ts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
