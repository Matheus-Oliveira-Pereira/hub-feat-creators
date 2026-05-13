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
@Table(name = "creator_profile_features")
public class CreatorProfileFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "influenciador_id", unique = true, nullable = false)
    private UUID influenciadorId;

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(name = "vertical_inferido")
    private String verticalInferido;

    @Column(name = "top_temas", columnDefinition = "TEXT[]")
    private String[] topTemas;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audience_geo", columnDefinition = "jsonb")
    private Map<String, Object> audienceGeo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "audience_demo", columnDefinition = "jsonb")
    private Map<String, Object> audienceDemo;

    @Column(name = "engagement_30d", precision = 8, scale = 4)
    private BigDecimal engagement30d;

    @Column(name = "freq_post_30d", precision = 6, scale = 2)
    private BigDecimal freqPost30d;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    protected CreatorProfileFeature() {}

    public CreatorProfileFeature(UUID influenciadorId, UUID assessoriaId) {
        this.influenciadorId = influenciadorId;
        this.assessoriaId = assessoriaId;
    }

    public void update(String verticalInferido, String[] topTemas,
            Map<String, Object> audienceGeo, Map<String, Object> audienceDemo,
            BigDecimal engagement30d, BigDecimal freqPost30d) {
        this.verticalInferido = verticalInferido;
        this.topTemas = topTemas;
        this.audienceGeo = audienceGeo;
        this.audienceDemo = audienceDemo;
        this.engagement30d = engagement30d;
        this.freqPost30d = freqPost30d;
        this.atualizadoEm = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getInfluenciadorId() { return influenciadorId; }
    public UUID getAssessoriaId() { return assessoriaId; }
    public String getVerticalInferido() { return verticalInferido; }
    public String[] getTopTemas() { return topTemas; }
    public Map<String, Object> getAudienceGeo() { return audienceGeo; }
    public Map<String, Object> getAudienceDemo() { return audienceDemo; }
    public BigDecimal getEngagement30d() { return engagement30d; }
    public BigDecimal getFreqPost30d() { return freqPost30d; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
}
