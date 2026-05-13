package com.hubfeatcreators.domain.match;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "match_model_versions")
public class MatchModelVersion {

    @Id
    private String versao;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pesos", nullable = false, columnDefinition = "jsonb")
    private Map<String, Double> pesos;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "ativada_em", nullable = false, updatable = false)
    private Instant ativadaEm = Instant.now();

    @Column(name = "desativada_em")
    private Instant desativadaEm;

    protected MatchModelVersion() {}

    public MatchModelVersion(String versao, Map<String, Double> pesos, String descricao) {
        this.versao = versao;
        this.pesos = pesos;
        this.descricao = descricao;
    }

    public void desativar() {
        this.desativadaEm = Instant.now();
    }

    public boolean isAtiva() {
        return desativadaEm == null;
    }

    public String getVersao() { return versao; }
    public Map<String, Double> getPesos() { return pesos; }
    public String getDescricao() { return descricao; }
    public Instant getAtivadaEm() { return ativadaEm; }
    public Instant getDesativadaEm() { return desativadaEm; }
}
