package com.hubfeatcreators.domain.match;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "match_sugestoes")
public class MatchSugestao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "prospeccao_id", nullable = false)
    private UUID prospeccaoId;

    @Column(name = "influenciador_id", nullable = false)
    private UUID influenciadorId;

    @Column(name = "score", nullable = false, precision = 6, scale = 4)
    private BigDecimal score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "razoes", nullable = false, columnDefinition = "jsonb")
    private List<Map<String, Object>> razoes;

    @Column(name = "modelo_versao", nullable = false)
    private String modeloVersao;

    @Column(name = "gerado_em", nullable = false, updatable = false)
    private Instant geradoEm = Instant.now();

    protected MatchSugestao() {}

    public MatchSugestao(
            UUID prospeccaoId,
            UUID influenciadorId,
            BigDecimal score,
            List<Map<String, Object>> razoes,
            String modeloVersao) {
        this.prospeccaoId = prospeccaoId;
        this.influenciadorId = influenciadorId;
        this.score = score;
        this.razoes = razoes;
        this.modeloVersao = modeloVersao;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProspeccaoId() {
        return prospeccaoId;
    }

    public UUID getInfluenciadorId() {
        return influenciadorId;
    }

    public BigDecimal getScore() {
        return score;
    }

    public List<Map<String, Object>> getRazoes() {
        return razoes;
    }

    public String getModeloVersao() {
        return modeloVersao;
    }

    public Instant getGeradoEm() {
        return geradoEm;
    }
}
