package com.hubfeatcreators.domain.match;

import com.hubfeatcreators.domain.influenciador.Influenciador;
import com.hubfeatcreators.domain.influenciador.InfluenciadorRepository;
import com.hubfeatcreators.infra.web.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
    private final InfluenciadorRepository influenciadorRepo;

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
            MatchExplainer explainer,
            InfluenciadorRepository influenciadorRepo) {
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
        this.influenciadorRepo = influenciadorRepo;
    }

    @Transactional
    public List<MatchSugestao> runMatch(UUID assessoriaId, UUID prospeccaoId) {
        Briefing briefing =
                briefingRepo
                        .findByProspeccaoId(prospeccaoId)
                        .orElseThrow(() -> BusinessException.notFound("BRIEFING"));

        MatchModelVersion model =
                modelRepo
                        .findActiveVersion()
                        .orElseThrow(() -> BusinessException.notFound("MATCH_MODEL_VERSION"));

        float[] briefingEmbedding = embeddingService.embed(buildBriefingText(briefing));
        vectorRepo.upsertBriefingEmbedding(briefing.getId(), briefingEmbedding);

        List<Map<String, Object>> candidates =
                vectorRepo.findSimilarCreators(assessoriaId, briefingEmbedding, TOP_K * 2);

        // Batch-load features and optouts to avoid N+1
        List<UUID> candidateIds =
                candidates.stream()
                        .map(c -> UUID.fromString(c.get("influenciador_id").toString()))
                        .toList();

        Set<UUID> optouts =
                optoutRepo.findAllById(candidateIds).stream()
                        .map(CreatorMatchOptout::getInfluenciadorId)
                        .collect(Collectors.toCollection(HashSet::new));

        List<UUID> eligibleIds = candidateIds.stream().filter(id -> !optouts.contains(id)).toList();
        if (eligibleIds.isEmpty()) return List.of();

        Map<UUID, CreatorProfileFeature> featureMap =
                featureRepo.findByInfluenciadorIdIn(eligibleIds).stream()
                        .collect(
                                Collectors.toMap(
                                        CreatorProfileFeature::getInfluenciadorId, f -> f));

        List<MatchSugestao> sugestoes = new ArrayList<>();
        for (Map<String, Object> candidate : candidates) {
            UUID influenciadorId = UUID.fromString(candidate.get("influenciador_id").toString());
            if (optouts.contains(influenciadorId)) continue;

            CreatorProfileFeature features = featureMap.get(influenciadorId);
            if (features == null) continue;

            double cosineSim = ((Number) candidate.get("cosine_sim")).doubleValue();
            int historicalDeals =
                    sugestaoRepo.countByInfluenciadorIdAndAssessoriaId(
                            influenciadorId, assessoriaId);
            double health = scorer.channelHealthScore(features);

            MatchScorer.ScoreInput input =
                    new MatchScorer.ScoreInput(
                            cosineSim, briefing, features, historicalDeals, health);
            double finalScore = scorer.score(input, model.getPesos());
            List<Map<String, Object>> razoes = explainer.explain(input, finalScore);

            Optional<MatchSugestao> existing =
                    sugestaoRepo.findByProspeccaoIdAndInfluenciadorIdAndModeloVersao(
                            prospeccaoId, influenciadorId, model.getVersao());
            if (existing.isPresent()) {
                sugestoes.add(existing.get());
                continue;
            }

            MatchSugestao sugestao =
                    new MatchSugestao(
                            assessoriaId,
                            prospeccaoId,
                            influenciadorId,
                            BigDecimal.valueOf(finalScore).setScale(4, RoundingMode.HALF_UP),
                            razoes,
                            model.getVersao());
            sugestoes.add(sugestaoRepo.save(sugestao));

            if (sugestoes.size() >= TOP_K) break;
        }

        return sugestoes;
    }

    @Transactional(readOnly = true)
    public List<MatchSugestao> getSugestoes(UUID assessoriaId, UUID prospeccaoId) {
        MatchModelVersion model =
                modelRepo
                        .findActiveVersion()
                        .orElseThrow(() -> BusinessException.notFound("MATCH_MODEL_VERSION"));
        List<MatchSugestao> list =
                sugestaoRepo.findByProspeccaoIdAndModeloVersaoOrderByScoreDesc(
                        prospeccaoId, model.getVersao());
        list.forEach(s -> assertAssessoria(assessoriaId, s.getAssessoriaId()));
        return list;
    }

    @Transactional(readOnly = true)
    public List<MatchSugestao> getReverse(UUID assessoriaId, UUID influenciadorId) {
        return sugestaoRepo.findByInfluenciadorIdAndAssessoriaIdOrderByScoreDesc(
                influenciadorId, assessoriaId);
    }

    @Transactional
    public MatchFeedback addFeedback(
            UUID assessoriaId, UUID sugestaoId, UUID autorId, String sinal, String comentario) {
        MatchSugestao sugestao =
                sugestaoRepo
                        .findById(sugestaoId)
                        .orElseThrow(() -> BusinessException.notFound("MATCH_SUGESTAO"));
        assertAssessoria(assessoriaId, sugestao.getAssessoriaId());
        return feedbackRepo.save(new MatchFeedback(sugestaoId, autorId, sinal, comentario));
    }

    @Transactional
    public void optout(UUID assessoriaId, UUID influenciadorId, String motivo) {
        Influenciador inf =
                influenciadorRepo
                        .findById(influenciadorId)
                        .orElseThrow(() -> BusinessException.notFound("INFLUENCIADOR"));
        if (!inf.getAssessoriaId().equals(assessoriaId))
            throw BusinessException.notFound("INFLUENCIADOR");
        if (!optoutRepo.existsById(influenciadorId)) {
            optoutRepo.save(new CreatorMatchOptout(influenciadorId, motivo));
        }
    }

    @Transactional
    public void optin(UUID assessoriaId, UUID influenciadorId) {
        Influenciador inf =
                influenciadorRepo
                        .findById(influenciadorId)
                        .orElseThrow(() -> BusinessException.notFound("INFLUENCIADOR"));
        if (!inf.getAssessoriaId().equals(assessoriaId))
            throw BusinessException.notFound("INFLUENCIADOR");
        optoutRepo.deleteById(influenciadorId);
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
