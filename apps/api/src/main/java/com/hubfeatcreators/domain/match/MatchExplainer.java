package com.hubfeatcreators.domain.match;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MatchExplainer {

    public List<Map<String, Object>> explain(
            MatchScorer.ScoreInput input, double finalScore) {
        List<Map<String, Object>> reasons = new ArrayList<>();

        double cosine = input.cosineSim();
        if (cosine >= 0.8) {
            reasons.add(reason("ALTA_SIMILARIDADE_SEMANTICA",
                    "Briefing e perfil do creator são altamente similares em conteúdo", cosine));
        } else if (cosine >= 0.5) {
            reasons.add(reason("SIMILARIDADE_SEMANTICA_MODERADA",
                    "Briefing e perfil do creator têm sobreposição temática", cosine));
        }

        Briefing b = input.briefing();
        CreatorProfileFeature f = input.features();
        if (b.getVertical() != null && f.getVerticalInferido() != null
                && b.getVertical().equalsIgnoreCase(f.getVerticalInferido())) {
            reasons.add(reason("VERTICAL_COMPATIVEL",
                    "Creator atua na vertical " + f.getVerticalInferido(), 1.0));
        }

        if (input.historicalDeals() > 0) {
            reasons.add(reason("HISTORICO_PARCERIAS",
                    "Creator já fechou " + input.historicalDeals() + " prospecção(ões) com esta assessoria",
                    Math.min(1.0, input.historicalDeals() / 3.0)));
        }

        double health = input.channelHealthScore();
        if (health >= 0.7) {
            reasons.add(reason("CANAL_SAUDAVEL",
                    "Canal com bom engajamento e frequência de postagens", health));
        }

        if (f.getEngagement30d() != null && f.getEngagement30d().doubleValue() >= 5.0) {
            reasons.add(reason("ALTO_ENGAJAMENTO",
                    String.format("Taxa de engajamento de %.1f%% nos últimos 30 dias",
                            f.getEngagement30d().doubleValue()), 1.0));
        }

        return reasons;
    }

    private Map<String, Object> reason(String tipo, String descricao, double peso) {
        return Map.of("tipo", tipo, "descricao", descricao, "peso", peso);
    }
}
