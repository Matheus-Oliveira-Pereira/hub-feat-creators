package com.hubfeatcreators.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubfeatcreators.domain.match.Briefing;
import com.hubfeatcreators.domain.match.CreatorProfileFeature;
import com.hubfeatcreators.domain.match.MatchExplainer;
import com.hubfeatcreators.domain.match.MatchScorer;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchExplainerTest {

    private final MatchExplainer explainer = new MatchExplainer();

    @Test
    void explain_high_cosine_produces_alta_similaridade() {
        MatchScorer.ScoreInput input = input(0.85, null, null, 0, 0.5);

        List<Map<String, Object>> reasons = explainer.explain(input, 0.85);

        assertThat(reasons).anyMatch(r -> "ALTA_SIMILARIDADE_SEMANTICA".equals(r.get("tipo")));
    }

    @Test
    void explain_moderate_cosine_produces_similaridade_moderada() {
        MatchScorer.ScoreInput input = input(0.6, null, null, 0, 0.5);

        List<Map<String, Object>> reasons = explainer.explain(input, 0.6);

        assertThat(reasons).anyMatch(r -> "SIMILARIDADE_SEMANTICA_MODERADA".equals(r.get("tipo")));
        assertThat(reasons).noneMatch(r -> "ALTA_SIMILARIDADE_SEMANTICA".equals(r.get("tipo")));
    }

    @Test
    void explain_low_cosine_produces_no_semantic_reason() {
        MatchScorer.ScoreInput input = input(0.3, null, null, 0, 0.5);

        List<Map<String, Object>> reasons = explainer.explain(input, 0.3);

        assertThat(reasons).noneMatch(r -> r.get("tipo").toString().contains("SIMILARIDADE"));
    }

    @Test
    void explain_matching_vertical_produces_vertical_compativel() {
        Briefing b = briefing("MODA");
        CreatorProfileFeature f = features("MODA", null, null);
        MatchScorer.ScoreInput input = input(0.5, b, f, 0, 0.5);

        List<Map<String, Object>> reasons = explainer.explain(input, 0.5);

        assertThat(reasons).anyMatch(r -> "VERTICAL_COMPATIVEL".equals(r.get("tipo")));
    }

    @Test
    void explain_mismatched_vertical_no_vertical_reason() {
        Briefing b = briefing("MODA");
        CreatorProfileFeature f = features("TECH", null, null);
        MatchScorer.ScoreInput input = input(0.5, b, f, 0, 0.5);

        List<Map<String, Object>> reasons = explainer.explain(input, 0.5);

        assertThat(reasons).noneMatch(r -> "VERTICAL_COMPATIVEL".equals(r.get("tipo")));
    }

    @Test
    void explain_historical_deals_produces_historico_parcerias() {
        MatchScorer.ScoreInput input = input(0.5, null, null, 2, 0.5);

        List<Map<String, Object>> reasons = explainer.explain(input, 0.5);

        assertThat(reasons).anyMatch(r -> "HISTORICO_PARCERIAS".equals(r.get("tipo")));
    }

    @Test
    void explain_zero_historical_deals_no_historico_reason() {
        MatchScorer.ScoreInput input = input(0.5, null, null, 0, 0.5);

        List<Map<String, Object>> reasons = explainer.explain(input, 0.5);

        assertThat(reasons).noneMatch(r -> "HISTORICO_PARCERIAS".equals(r.get("tipo")));
    }

    @Test
    void explain_high_health_produces_canal_saudavel() {
        MatchScorer.ScoreInput input = input(0.5, null, null, 0, 0.8);

        List<Map<String, Object>> reasons = explainer.explain(input, 0.5);

        assertThat(reasons).anyMatch(r -> "CANAL_SAUDAVEL".equals(r.get("tipo")));
    }

    @Test
    void explain_high_engagement_feature_produces_alto_engajamento() {
        CreatorProfileFeature f = features(null, BigDecimal.valueOf(6.0), null);
        MatchScorer.ScoreInput input = input(0.5, null, f, 0, 0.5);

        List<Map<String, Object>> reasons = explainer.explain(input, 0.5);

        assertThat(reasons).anyMatch(r -> "ALTO_ENGAJAMENTO".equals(r.get("tipo")));
    }

    @Test
    void explain_all_thresholds_met_returns_multiple_reasons() {
        Briefing b = briefing("MODA");
        CreatorProfileFeature f = features("MODA", BigDecimal.valueOf(7.0), null);
        MatchScorer.ScoreInput input = input(0.9, b, f, 3, 0.85);

        List<Map<String, Object>> reasons = explainer.explain(input, 0.9);

        assertThat(reasons.size()).isGreaterThanOrEqualTo(4);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private MatchScorer.ScoreInput input(
            double cosine,
            Briefing briefing,
            CreatorProfileFeature features,
            int deals,
            double health) {
        Briefing b = briefing != null ? briefing : emptyBriefing();
        CreatorProfileFeature f = features != null ? features : emptyFeatures();
        return new MatchScorer.ScoreInput(cosine, b, f, deals, health);
    }

    private Briefing briefing(String vertical) {
        return new Briefing(
                UUID.randomUUID(), UUID.randomUUID(), vertical, null, null, null, null, null, null);
    }

    private Briefing emptyBriefing() {
        return new Briefing(
                UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, null, null, null);
    }

    private CreatorProfileFeature features(
            String vertical, BigDecimal engagement, BigDecimal freq) {
        CreatorProfileFeature f = new CreatorProfileFeature(UUID.randomUUID(), UUID.randomUUID());
        f.update(vertical, null, null, null, engagement, freq);
        return f;
    }

    private CreatorProfileFeature emptyFeatures() {
        return new CreatorProfileFeature(UUID.randomUUID(), UUID.randomUUID());
    }
}
