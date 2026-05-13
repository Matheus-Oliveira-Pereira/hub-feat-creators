package com.hubfeatcreators.domain.match;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "briefings")
public class Briefing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(name = "prospeccao_id", unique = true)
    private UUID prospeccaoId;

    @Column(name = "vertical")
    private String vertical;

    @Column(name = "objetivo")
    private String objetivo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audiencia", columnDefinition = "jsonb")
    private Map<String, Object> audiencia;

    @Column(name = "formato")
    private String formato;

    @Column(name = "budget_min")
    private Long budgetMin;

    @Column(name = "budget_max")
    private Long budgetMax;

    @Column(name = "texto", columnDefinition = "TEXT")
    private String texto;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Briefing() {}

    public Briefing(UUID assessoriaId, UUID prospeccaoId, String vertical, String objetivo,
            Map<String, Object> audiencia, String formato, Long budgetMin, Long budgetMax,
            String texto) {
        this.assessoriaId = assessoriaId;
        this.prospeccaoId = prospeccaoId;
        this.vertical = vertical;
        this.objetivo = objetivo;
        this.audiencia = audiencia;
        this.formato = formato;
        this.budgetMin = budgetMin;
        this.budgetMax = budgetMax;
        this.texto = texto;
    }

    public void update(String vertical, String objetivo, Map<String, Object> audiencia,
            String formato, Long budgetMin, Long budgetMax, String texto) {
        this.vertical = vertical;
        this.objetivo = objetivo;
        this.audiencia = audiencia;
        this.formato = formato;
        this.budgetMin = budgetMin;
        this.budgetMax = budgetMax;
        this.texto = texto;
        this.atualizadoEm = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getAssessoriaId() { return assessoriaId; }
    public UUID getProspeccaoId() { return prospeccaoId; }
    public String getVertical() { return vertical; }
    public String getObjetivo() { return objetivo; }
    public Map<String, Object> getAudiencia() { return audiencia; }
    public String getFormato() { return formato; }
    public Long getBudgetMin() { return budgetMin; }
    public Long getBudgetMax() { return budgetMax; }
    public String getTexto() { return texto; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public Instant getCreatedAt() { return createdAt; }
}
