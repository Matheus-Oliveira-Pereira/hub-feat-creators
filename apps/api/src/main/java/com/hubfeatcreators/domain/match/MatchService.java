package com.hubfeatcreators.domain.match;

import com.hubfeatcreators.infra.web.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchService {

    private static final int TOP_K = 20;

    private final BriefingRepository briefingRepo;
    private final CreatorProfileFeatureRepository featureRepo;
    private final MatchSugestaoRepository sugestaoRepo;
    private final MatchFeedbackRepository feedbackRepo;
    private final MatchModelVersionRepository modelRepo;
    private final CreatorMatchOptoutRepository optoutRepo;
    private final VectorRepository vectorRepo;
    private final EmbeddingService embeddingService;
    private final MatchScorer scorer;
    private final MatchExplainer explainer;

    public MatchService(
            BriefingRepository briefingRepo,
            CreatorProfileFeatureRepository featureRepo,
            MatchSugestaoRepository sugestaoRepo,
            MatchFeedbackRepository feedbackRepo,
            MatchModelVersionRepository modelRepo,
            CreatorMatchOptoutRepository optoutRepo,
            VectorRepository vectorRepo,
            EmbeddingService embeddingService,
            MatchScorer scorer,
            MatchExplainer explainer) {
        this.briefingRepo = briefingRepo;
        this.featureRepo = featureRepo;
        this.sugestaoRepo = sugestaoRepo;
        this.feedbackRepo = feedbackRepo;
        this.modelRepo = modelRepo;
        this.optoutRepo = optoutRepo;
        this.vectorRepo = vectorRepo;
        this.embeddingService = embeddingService;
        this.scorer = scorer;
        this.explainer = explainer;
    }

    @Transactional
    public List<MatchSugestao> runMatch(UUID assessoriaId, UUID prospeccaoId) {
        Briefing briefing = briefingRepo
                .findByProspeccaoId(prospeccaoId)
                .orElseThrow(() -> BusinessException.notFound("BRIEFING"));

        MatchModelVersion model = modelRepo
                .findActiveVersion()
                .orElseThrow(() -> BusinessException.notFound("MATCH_MODEL_VERSION"));

        float[] briefingEmbedding = embeddingService.embed(buildBriefingText(briefing));
        vectorRepo.upsertBriefingEmbedding(briefing.getId(), briefingEmbedding);

        List<Map<String, Object>> candidates =
                vectorRepo.findSimilarCreators(assessoriaId, briefingEmbedding, TOP_K * 2);

        List<MatchSugestao> sugestoes = new ArrayList<>();
        for (Map<String, Object> candidate : candidates) {
            UUID influenciadorId = UUID.fromString(candidate.get("influenciador_id").toString());
            if (optoutRepo.existsById(influenciadorId)) continue;

            Optional<CreatorProfileFeature> featOpt = featureRepo.findByInfluenciadorId(influenciadorId);
            if (featOpt.isEmpty()) continue;

            CreatorProfileFeature features = featOpt.get();
            double cosineSim = ((Number) candidate.get("cosine_sim")).doubleValue();
            int historicalDeals = countHistoricalDeals(assessoriaId, influenciadorId);
            double health = scorer.channelHealthScore(features);

            MatchScorer.ScoreInput input = new MatchScorer.ScoreInput(
                    cosineSim, briefing, features, historicalDeals, health);
            double finalScore = scorer.score(input, model.getPesos());

            List<Map<String, Object>> razoes = explainer.explain(input, finalScore);

            MatchSugestao existing = sugestaoRepo
                    .findByProspeccaoIdAndInfluenciadorIdAndModeloVersao(
                            prospeccaoId, influenciadorId, model.getVersao())
                    .orElse(null);
            if (existing != null) {
                sugestoes.add(existing);
                continue;
            }

            MatchSugestao sugestao = new MatchSugestao(
                    assessoriaId, prospeccaoId, influenciadorId,
                    BigDecimal.valueOf(finalScore).setScale(4, RoundingMode.HALF_UP),
                    razoes, model.getVersao());
            sugestoes.add(sugestaoRepo.save(sugestao));

            if (sugestoes.size() >= TOP_K) break;
        }

        return sugestoes;
    }

    @Transactional(readOnly = true)
    public List<MatchSugestao> getSugestoes(UUID assessoriaId, UUID prospeccaoId) {
        MatchModelVersion model = modelRepo.findActiveVersion()
                .orElseThrow(() -> BusinessException.notFound("MATCH_MODEL_VERSION"));
        List<MatchSugestao> list = sugestaoRepo
                .findByProspeccaoIdAndModeloVersaoOrderByScoreDesc(prospeccaoId, model.getVersao());
        list.forEach(s -> assertAssessoria(assessoriaId, s.getAssessoriaId()));
        return list;
    }

    @Transactional(readOnly = true)
    public List<MatchSugestao> getReverse(UUID assessoriaId, UUID influenciadorId) {
        List<MatchSugestao> list = sugestaoRepo.findByInfluenciadorIdOrderByScoreDesc(influenciadorId);
        list.forEach(s -> assertAssessoria(assessoriaId, s.getAssessoriaId()));
        return list;
    }

    @Transactional
    public MatchFeedback addFeedback(UUID assessoriaId, UUID sugestaoId, UUID autorId,
            String sinal, String comentario) {
        MatchSugestao sugestao = sugestaoRepo.findById(sugestaoId)
                .orElseThrow(() -> BusinessException.notFound("MATCH_SUGESTAO"));
        assertAssessoria(assessoriaId, sugestao.getAssessoriaId());
        return feedbackRepo.save(new MatchFeedback(sugestaoId, autorId, sinal, comentario));
    }

    @Transactional
    public void optout(UUID influenciadorId, String motivo) {
        if (!optoutRepo.existsById(influenciadorId)) {
            optoutRepo.save(new CreatorMatchOptout(influenciadorId, motivo));
        }
    }

    @Transactional
    public void optin(UUID influenciadorId) {
        optoutRepo.deleteById(influenciadorId);
    }

    private int countHistoricalDeals(UUID assessoriaId, UUID influenciadorId) {
        return sugestaoRepo.findByInfluenciadorIdOrderByScoreDesc(influenciadorId).stream()
                .filter(s -> s.getAssessoriaId().equals(assessoriaId))
                .mapToInt(s -> 1)
                .sum();
    }

    private String buildBriefingText(Briefing briefing) {
        StringBuilder sb = new StringBuilder();
        if (briefing.getVertical() != null) sb.append(briefing.getVertical()).append(" ");
        if (briefing.getObjetivo() != null) sb.append(briefing.getObjetivo()).append(" ");
        if (briefing.getFormato() != null) sb.append(briefing.getFormato()).append(" ");
        if (briefing.getTexto() != null) sb.append(briefing.getTexto());
        return sb.toString().trim();
    }

    private void assertAssessoria(UUID expected, UUID actual) {
        if (!expected.equals(actual)) throw BusinessException.notFound("MATCH_SUGESTAO");
    }
}
