package com.hubfeatcreators.domain.match;

import com.hubfeatcreators.domain.social.SocialAccount;
import com.hubfeatcreators.domain.social.SocialAccountRepository;
import com.hubfeatcreators.domain.social.SocialPostRepository;
import com.hubfeatcreators.domain.social.SocialSnapshot;
import com.hubfeatcreators.domain.social.SocialSnapshotRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class CreatorFeatureBuilder {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(CreatorFeatureBuilder.class);

    private final SocialAccountRepository accountRepo;
    private final SocialSnapshotRepository snapshotRepo;
    private final SocialPostRepository postRepo;
    private final CreatorProfileFeatureRepository featureRepo;
    private final EmbeddingService embeddingService;
    private final VectorRepository vectorRepo;

    public CreatorFeatureBuilder(
            SocialAccountRepository accountRepo,
            SocialSnapshotRepository snapshotRepo,
            SocialPostRepository postRepo,
            CreatorProfileFeatureRepository featureRepo,
            EmbeddingService embeddingService,
            VectorRepository vectorRepo) {
        this.accountRepo = accountRepo;
        this.snapshotRepo = snapshotRepo;
        this.postRepo = postRepo;
        this.featureRepo = featureRepo;
        this.embeddingService = embeddingService;
        this.vectorRepo = vectorRepo;
    }

    public CreatorProfileFeature buildAndSave(UUID influenciadorId) {
        List<SocialAccount> accounts =
                accountRepo.findByInfluenciadorId(influenciadorId);

        BigDecimal avgEngagement = computeEngagement(accounts);
        BigDecimal freqPost = computeFreqPost(accounts);
        String vertical = inferVertical(accounts);
        String[] topTemas = inferTopTemas(accounts);

        CreatorProfileFeature feature =
                featureRepo
                        .findByInfluenciadorId(influenciadorId)
                        .orElseGet(() -> new CreatorProfileFeature(influenciadorId));

        feature.update(vertical, topTemas, null, null, avgEngagement, freqPost);
        featureRepo.save(feature);

        String profileText = buildProfileText(vertical, topTemas, avgEngagement);
        float[] embedding = embeddingService.embed(profileText);
        vectorRepo.upsertCreatorEmbedding(influenciadorId, embedding);

        return feature;
    }

    private BigDecimal computeEngagement(List<SocialAccount> accounts) {
        return accounts.stream()
                .flatMap(
                        a ->
                                snapshotRepo
                                        .findBySocialAccountIdOrderByDiaDesc(
                                                a.getId(), PageRequest.of(0, 30))
                                        .stream())
                .filter(s -> s.getEngagementRate() != null)
                .map(SocialSnapshot::getEngagementRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, accounts.size())), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal computeFreqPost(List<SocialAccount> accounts) {
        long totalPosts =
                accounts.stream()
                        .flatMap(
                                a ->
                                        postRepo
                                                .findTop30BySocialAccountIdOrderByPostedAtDesc(
                                                        a.getId())
                                                .stream())
                        .count();
        return BigDecimal.valueOf(totalPosts)
                .divide(BigDecimal.valueOf(Math.max(1, accounts.size())), 2, RoundingMode.HALF_UP);
    }

    // TODO(PRD-016 Fase 2): inferir vertical a partir de hashtags/categoria dos SocialPost.
    // Hoje retorna null → componente categórico do MatchScorer fica em 0.5 (neutro).
    // Match continua funcionando via similaridade vetorial + histórico + saúde do canal.
    private String inferVertical(List<SocialAccount> accounts) {
        log.warn("creator.features.inferVertical.stub accounts={} → null", accounts.size());
        return null;
    }

    // TODO(PRD-016 Fase 2): extrair top temas dos captions dos SocialPost via TF-IDF ou LLM.
    // Hoje retorna [] → buildProfileText não inclui temas no embedding.
    private String[] inferTopTemas(List<SocialAccount> accounts) {
        log.warn("creator.features.inferTopTemas.stub accounts={} → []", accounts.size());
        return new String[0];
    }

    private String buildProfileText(String vertical, String[] temas, BigDecimal engagement) {
        StringBuilder sb = new StringBuilder();
        if (vertical != null) sb.append("Vertical: ").append(vertical).append(". ");
        if (temas != null && temas.length > 0) {
            sb.append("Temas: ").append(String.join(", ", temas)).append(". ");
        }
        if (engagement != null) {
            sb.append("Engajamento: ").append(engagement).append("%.");
        }
        return sb.toString();
    }
}
