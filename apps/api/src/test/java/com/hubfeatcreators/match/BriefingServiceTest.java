package com.hubfeatcreators.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.hubfeatcreators.domain.match.Briefing;
import com.hubfeatcreators.domain.match.BriefingRepository;
import com.hubfeatcreators.domain.match.BriefingService;
import com.hubfeatcreators.domain.match.EmbeddingService;
import com.hubfeatcreators.domain.match.VectorRepository;
import com.hubfeatcreators.infra.web.BusinessException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BriefingServiceTest {

    @Mock BriefingRepository briefingRepo;
    @Mock EmbeddingService embeddingService;
    @Mock VectorRepository vectorRepo;

    @InjectMocks BriefingService service;

    UUID assessoriaId = UUID.randomUUID();
    UUID prospeccaoId = UUID.randomUUID();

    @Test
    void upsert_creates_new_briefing_when_none_exists() {
        when(briefingRepo.findByProspeccaoId(prospeccaoId)).thenReturn(Optional.empty());
        when(briefingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(embeddingService.embed(any())).thenReturn(new float[384]);

        Briefing result =
                service.upsertByProspeccao(
                        assessoriaId,
                        prospeccaoId,
                        "MODA",
                        "AWARENESS",
                        null,
                        "REELS",
                        null,
                        null,
                        "texto");

        assertThat(result.getAssessoriaId()).isEqualTo(assessoriaId);
        assertThat(result.getProspeccaoId()).isEqualTo(prospeccaoId);
        assertThat(result.getVertical()).isEqualTo("MODA");
        verify(vectorRepo).upsertBriefingEmbedding(any(), any());
    }

    @Test
    void upsert_updates_existing_briefing() {
        Briefing existing =
                new Briefing(
                        assessoriaId,
                        prospeccaoId,
                        "MODA",
                        "AWARENESS",
                        null,
                        "REELS",
                        null,
                        null,
                        "old");
        when(briefingRepo.findByProspeccaoId(prospeccaoId)).thenReturn(Optional.of(existing));
        when(briefingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(embeddingService.embed(any())).thenReturn(new float[384]);

        Briefing result =
                service.upsertByProspeccao(
                        assessoriaId,
                        prospeccaoId,
                        "TECH",
                        "CONVERSAO",
                        null,
                        "VIDEO",
                        null,
                        null,
                        "novo texto");

        assertThat(result.getVertical()).isEqualTo("TECH");
        assertThat(result.getTexto()).isEqualTo("novo texto");
        verify(vectorRepo).upsertBriefingEmbedding(any(), any());
    }

    @Test
    void upsert_throws_when_existing_briefing_belongs_to_other_assessoria() {
        UUID otherId = UUID.randomUUID();
        Briefing existing =
                new Briefing(
                        otherId, prospeccaoId, "MODA", "AWARENESS", null, "REELS", null, null, "x");
        when(briefingRepo.findByProspeccaoId(prospeccaoId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(
                        () ->
                                service.upsertByProspeccao(
                                        assessoriaId,
                                        prospeccaoId,
                                        "TECH",
                                        "CONVERSAO",
                                        null,
                                        "VIDEO",
                                        null,
                                        null,
                                        "x"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void upsert_builds_embedding_text_from_fields() {
        when(briefingRepo.findByProspeccaoId(prospeccaoId)).thenReturn(Optional.empty());
        when(briefingRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(embeddingService.embed(any())).thenReturn(new float[384]);

        service.upsertByProspeccao(
                assessoriaId,
                prospeccaoId,
                "BELEZA",
                "REACH",
                null,
                "STORIES",
                null,
                null,
                "produto novo");

        ArgumentCaptor<String> textCaptor = ArgumentCaptor.forClass(String.class);
        verify(embeddingService).embed(textCaptor.capture());
        assertThat(textCaptor.getValue())
                .contains("BELEZA")
                .contains("REACH")
                .contains("STORIES")
                .contains("produto novo");
    }

    @Test
    void getByProspeccao_returns_briefing_for_correct_assessoria() {
        Briefing briefing =
                new Briefing(
                        assessoriaId,
                        prospeccaoId,
                        "MODA",
                        "AWARENESS",
                        null,
                        "REELS",
                        null,
                        null,
                        "x");
        when(briefingRepo.findByProspeccaoId(prospeccaoId)).thenReturn(Optional.of(briefing));

        Briefing result = service.getByProspeccao(assessoriaId, prospeccaoId);

        assertThat(result).isSameAs(briefing);
    }

    @Test
    void getByProspeccao_throws_when_not_found() {
        when(briefingRepo.findByProspeccaoId(prospeccaoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByProspeccao(assessoriaId, prospeccaoId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getByProspeccao_throws_when_assessoria_mismatch() {
        UUID otherId = UUID.randomUUID();
        Briefing briefing =
                new Briefing(
                        otherId, prospeccaoId, "MODA", "AWARENESS", null, "REELS", null, null, "x");
        when(briefingRepo.findByProspeccaoId(prospeccaoId)).thenReturn(Optional.of(briefing));

        assertThatThrownBy(() -> service.getByProspeccao(assessoriaId, prospeccaoId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void listByAssessoria_delegates_to_repo() {
        Briefing b =
                new Briefing(
                        assessoriaId,
                        prospeccaoId,
                        "MODA",
                        "AWARENESS",
                        null,
                        "REELS",
                        null,
                        null,
                        "x");
        when(briefingRepo.findByAssessoriaId(assessoriaId)).thenReturn(List.of(b));

        List<Briefing> result = service.listByAssessoria(assessoriaId);

        assertThat(result).hasSize(1);
        verify(briefingRepo).findByAssessoriaId(eq(assessoriaId));
    }
}
