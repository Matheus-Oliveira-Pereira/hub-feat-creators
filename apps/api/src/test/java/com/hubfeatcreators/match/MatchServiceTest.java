package com.hubfeatcreators.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hubfeatcreators.domain.match.*;
import com.hubfeatcreators.infra.web.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MatchServiceTest {

    @Mock BriefingRepository briefingRepo;
    @Mock CreatorProfileFeatureRepository featureRepo;
    @Mock MatchSugestaoRepository sugestaoRepo;
    @Mock MatchFeedbackRepository feedbackRepo;
    @Mock MatchModelVersionRepository modelRepo;
    @Mock CreatorMatchOptoutRepository optoutRepo;
    @Mock VectorRepository vectorRepo;
    @Mock EmbeddingService embeddingService;
    @Mock MatchScorer scorer;
    @Mock MatchExplainer explainer;

    @InjectMocks MatchService matchService;

    UUID assessoriaId = UUID.randomUUID();
    UUID prospeccaoId = UUID.randomUUID();
    UUID influenciadorId = UUID.randomUUID();

    @Test
    void runMatch_throws_when_briefing_not_found() {
        when(briefingRepo.findByProspeccaoId(prospeccaoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.runMatch(assessoriaId, prospeccaoId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void runMatch_throws_when_model_not_found() {
        Briefing briefing = new Briefing(assessoriaId, prospeccaoId, "MODA", "AWARENESS", null, "REELS", null, null, "texto");
        when(briefingRepo.findByProspeccaoId(prospeccaoId)).thenReturn(Optional.of(briefing));
        when(modelRepo.findActiveVersion()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.runMatch(assessoriaId, prospeccaoId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void runMatch_skips_opted_out_creators() {
        Briefing briefing = new Briefing(assessoriaId, prospeccaoId, "MODA", "AWARENESS", null, "REELS", null, null, "texto");
        MatchModelVersion model = new MatchModelVersion("v1.0", Map.of("w1", 0.40, "w2", 0.25, "w3", 0.20, "w4", 0.15), "test");

        when(briefingRepo.findByProspeccaoId(prospeccaoId)).thenReturn(Optional.of(briefing));
        when(modelRepo.findActiveVersion()).thenReturn(Optional.of(model));
        when(embeddingService.embed(any())).thenReturn(new float[384]);
        when(vectorRepo.findSimilarCreators(eq(assessoriaId), any(), anyInt()))
                .thenReturn(List.of(Map.of("influenciador_id", influenciadorId.toString(), "cosine_sim", 0.9)));
        when(optoutRepo.existsById(influenciadorId)).thenReturn(true);

        List<MatchSugestao> result = matchService.runMatch(assessoriaId, prospeccaoId);

        assertThat(result).isEmpty();
        verifyNoInteractions(featureRepo);
    }

    @Test
    void runMatch_creates_sugestoes_for_eligible_creators() {
        Briefing briefing = new Briefing(assessoriaId, prospeccaoId, "MODA", "AWARENESS", null, "REELS", null, null, "texto");
        MatchModelVersion model = new MatchModelVersion("v1.0", Map.of("w1", 0.40, "w2", 0.25, "w3", 0.20, "w4", 0.15), "test");
        CreatorProfileFeature features = new CreatorProfileFeature(influenciadorId, assessoriaId);
        MatchSugestao sugestao = new MatchSugestao(assessoriaId, prospeccaoId, influenciadorId,
                BigDecimal.valueOf(0.85), List.of(), "v1.0");

        when(briefingRepo.findByProspeccaoId(prospeccaoId)).thenReturn(Optional.of(briefing));
        when(modelRepo.findActiveVersion()).thenReturn(Optional.of(model));
        when(embeddingService.embed(any())).thenReturn(new float[384]);
        when(vectorRepo.findSimilarCreators(eq(assessoriaId), any(), anyInt()))
                .thenReturn(List.of(Map.of("influenciador_id", influenciadorId.toString(), "cosine_sim", 0.85)));
        when(optoutRepo.existsById(influenciadorId)).thenReturn(false);
        when(featureRepo.findByInfluenciadorId(influenciadorId)).thenReturn(Optional.of(features));
        when(sugestaoRepo.findByInfluenciadorIdOrderByScoreDesc(influenciadorId)).thenReturn(List.of());
        when(sugestaoRepo.findByProspeccaoIdAndInfluenciadorIdAndModeloVersao(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(scorer.score(any(), any())).thenReturn(0.85);
        when(scorer.channelHealthScore(any())).thenReturn(0.7);
        when(explainer.explain(any(), anyDouble())).thenReturn(List.of());
        when(sugestaoRepo.save(any())).thenReturn(sugestao);

        List<MatchSugestao> result = matchService.runMatch(assessoriaId, prospeccaoId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getInfluenciadorId()).isEqualTo(influenciadorId);
    }

    @Test
    void addFeedback_throws_for_wrong_assessoria() {
        UUID otherAssessoria = UUID.randomUUID();
        UUID sugestaoId = UUID.randomUUID();
        MatchSugestao sugestao = new MatchSugestao(otherAssessoria, prospeccaoId, influenciadorId,
                BigDecimal.valueOf(0.7), List.of(), "v1.0");
        when(sugestaoRepo.findById(sugestaoId)).thenReturn(Optional.of(sugestao));

        assertThatThrownBy(() -> matchService.addFeedback(assessoriaId, sugestaoId, UUID.randomUUID(), "BOA", null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void optout_saves_record() {
        when(optoutRepo.existsById(influenciadorId)).thenReturn(false);

        matchService.optout(influenciadorId, "nao quero");

        verify(optoutRepo).save(any(CreatorMatchOptout.class));
    }

    @Test
    void optout_idempotent_when_already_opted_out() {
        when(optoutRepo.existsById(influenciadorId)).thenReturn(true);

        matchService.optout(influenciadorId, "nao quero");

        verify(optoutRepo, never()).save(any());
    }
}
