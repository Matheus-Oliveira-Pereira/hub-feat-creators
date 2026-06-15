package com.hubfeatcreators.domain.match;

import com.hubfeatcreators.infra.web.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BriefingService {

    private final BriefingRepository briefingRepo;
    private final EmbeddingService embeddingService;
    private final VectorRepository vectorRepo;

    public BriefingService(
            BriefingRepository briefingRepo,
            EmbeddingService embeddingService,
            VectorRepository vectorRepo) {
        this.briefingRepo = briefingRepo;
        this.embeddingService = embeddingService;
        this.vectorRepo = vectorRepo;
    }

    @Transactional
    public Briefing upsertByProspeccao(
            UUID prospeccaoId,
            String vertical,
            String objetivo,
            Map<String, Object> audiencia,
            String formato,
            Long budgetMin,
            Long budgetMax,
            String texto) {
        Briefing briefing = briefingRepo.findByProspeccaoId(prospeccaoId).orElse(null);
        if (briefing == null) {
            briefing =
                    new Briefing(
                            prospeccaoId,
                            vertical,
                            objetivo,
                            audiencia,
                            formato,
                            budgetMin,
                            budgetMax,
                            texto);
        } else {
            briefing.update(vertical, objetivo, audiencia, formato, budgetMin, budgetMax, texto);
        }
        briefing = briefingRepo.save(briefing);

        String text = buildText(vertical, objetivo, formato, texto);
        float[] embedding = embeddingService.embed(text);
        vectorRepo.upsertBriefingEmbedding(briefing.getId(), embedding);

        return briefing;
    }

    @Transactional(readOnly = true)
    public Briefing getByProspeccao(UUID prospeccaoId) {
        return briefingRepo
                .findByProspeccaoId(prospeccaoId)
                .orElseThrow(() -> BusinessException.notFound("BRIEFING"));
    }

    @Transactional(readOnly = true)
    public List<Briefing> listAll() {
        return briefingRepo.findAll();
    }

    private String buildText(String vertical, String objetivo, String formato, String texto) {
        return String.join(
                        " ",
                        vertical != null ? vertical : "",
                        objetivo != null ? objetivo : "",
                        formato != null ? formato : "",
                        texto != null ? texto : "")
                .trim();
    }
}
