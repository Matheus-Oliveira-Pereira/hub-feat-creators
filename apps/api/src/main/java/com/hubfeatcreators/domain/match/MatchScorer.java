package com.hubfeatcreators.domain.match;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MatchScorer {

    public record ScoreInput(
            double cosineSim,
            Briefing briefing,
            CreatorProfileFeature features,
            int historicalDeals,
            double channelHealthScore) {}

    public double score(ScoreInput input, Map<String, Double> weights) {
        double w1 = weights.getOrDefault("w1", 0.40);
        double w2 = weights.getOrDefault("w2", 0.25);
        double w3 = weights.getOrDefault("w3", 0.20);
        double w4 = weights.getOrDefault("w4", 0.15);

        double vectorScore = Math.max(0, Math.min(1, input.cosineSim()));
        double categoricalScore = categoricalMatch(input.briefing(), input.features());
        double historyScore = historyScore(input.historicalDeals());
        double healthScore = Math.max(0, Math.min(1, input.channelHealthScore()));

        return w1 * vectorScore + w2 * categoricalScore + w3 * historyScore + w4 * healthScore;
    }

    private double categoricalMatch(Briefing briefing, CreatorProfileFeature features) {
        if (briefing.getVertical() == null || features.getVerticalInferido() == null) return 0.5;
        boolean verticalMatch =
                briefing.getVertical().equalsIgnoreCase(features.getVerticalInferido());
        boolean formatMatch =
                briefing.getFormato() == null || "QUALQUER".equals(briefing.getFormato());
        return (verticalMatch ? 0.7 : 0.2) + (formatMatch ? 0.3 : 0.0);
    }

    private double historyScore(int deals) {
        if (deals == 0) return 0.3;
        if (deals == 1) return 0.6;
        if (deals <= 3) return 0.8;
        return 1.0;
    }

    public double channelHealthScore(CreatorProfileFeature features) {
        if (features == null) return 0.5;
        BigDecimal eng = features.getEngagement30d();
        BigDecimal freq = features.getFreqPost30d();
        double engScore = eng == null ? 0.5 : Math.min(1.0, eng.doubleValue() / 10.0);
        double freqScore = freq == null ? 0.5 : Math.min(1.0, freq.doubleValue() / 30.0);
        return (engScore + freqScore) / 2.0;
    }
}
