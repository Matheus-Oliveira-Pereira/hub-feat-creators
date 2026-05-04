package com.hubfeatcreators.domain.compliance;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "consentimentos")
public class Consentimento {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(name = "titular_tipo", nullable = false)
    private String titularTipo;

    @Column(name = "titular_id", nullable = false)
    private UUID titularId;

    @Column(nullable = false)
    private String finalidade;

    @Column(name = "dado_em", nullable = false)
    private Instant dadoEm = Instant.now();

    @Column(name = "revogado_em")
    private Instant revogadoEm;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> prova = Map.of();

    public Consentimento() {}

    public Consentimento(UUID assessoriaId, String titularTipo, UUID titularId, String finalidade, Map<String, Object> prova) {
        this.assessoriaId = assessoriaId;
        this.titularTipo = titularTipo;
        this.titularId = titularId;
        this.finalidade = finalidade;
        this.prova = prova;
    }

    public UUID getId() { return id; }
    public UUID getAssessoriaId() { return assessoriaId; }
    public String getTitularTipo() { return titularTipo; }
    public UUID getTitularId() { return titularId; }
    public String getFinalidade() { return finalidade; }
    public Instant getDadoEm() { return dadoEm; }
    public Instant getRevogadoEm() { return revogadoEm; }
    public void setRevogadoEm(Instant revogadoEm) { this.revogadoEm = revogadoEm; }
    public Map<String, Object> getProva() { return prova; }
}
