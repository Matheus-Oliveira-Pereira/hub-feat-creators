package com.hubfeatcreators.match;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubfeatcreators.domain.match.Briefing;
import com.hubfeatcreators.domain.match.CreatorProfileFeature;
import com.hubfeatcreators.domain.match.MatchScorer;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MatchScorerTest {

    private final MatchScorer scorer = new MatchScorer();
    private final Map<String, Double> weights =
            Map.of("w1", 0.40, "w2", 0.25, "w3", 0.20, "w4", 0.15);

    @Test
    void score_perfect_match() {
        Briefing briefing = buildBriefing("MODA", "AWARENESS", "REELS");
        CreatorProfileFeature features =
                buildFeatures("MODA", BigDecimal.valueOf(8.0), BigDecimal.valueOf(25));

        MatchScorer.ScoreInput input = new MatchScorer.ScoreInput(1.0, briefing, features, 3, 0.9);
        double score = scorer.score(input, weights);

        assertThat(score).isGreaterThan(0.8);
    }

    @Test
    void score_zero_similarity_lowers_total() {
        Briefing briefing = buildBriefing("TECH", "CONVERSAO", "VIDEO");
        CreatorProfileFeature features =
                buildFeatures("MODA", BigDecimal.valueOf(3.0), BigDecimal.valueOf(10));

        MatchScorer.ScoreInput input = new MatchScorer.ScoreInput(0.0, briefing, features, 0, 0.3);
        double score = scorer.score(input, weights);

        assertThat(score).isLessThan(0.5);
    }

    @Test
    void channelHealthScore_uses_engagement_and_freq() {
        CreatorProfileFeature features =
                new CreatorProfileFeature(UUID.randomUUID(), UUID.randomUUID());
        features.update(null, null, null, null, BigDecimal.valueOf(5.0), BigDecimal.valueOf(15));

        double health = scorer.channelHealthScore(features);

        assertThat(health).isBetween(0.4, 0.8);
    }

    @Test
    void channelHealthScore_null_features_returns_half() {
        assertThat(scorer.channelHealthScore(null)).isEqualTo(0.5);
    }

    private Briefing buildBriefing(String vertical, String objetivo, String formato) {
        return new Briefing(
                UUID.randomUUID(),
                UUID.randomUUID(),
                vertical,
                objetivo,
                null,
                formato,
                null,
                null,
                null);
    }

    private CreatorProfileFeature buildFeatures(
            String vertical, BigDecimal engagement, BigDecimal freq) {
        CreatorProfileFeature f = new CreatorProfileFeature(UUID.randomUUID(), UUID.randomUUID());
        f.update(vertical, null, null, null, engagement, freq);
        return f;
    }
}
