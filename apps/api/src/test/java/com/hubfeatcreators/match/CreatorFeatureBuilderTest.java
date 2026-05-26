package com.hubfeatcreators.match;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.hubfeatcreators.domain.match.CreatorFeatureBuilder;
import com.hubfeatcreators.domain.match.CreatorProfileFeature;
import com.hubfeatcreators.domain.match.CreatorProfileFeatureRepository;
import com.hubfeatcreators.domain.match.EmbeddingService;
import com.hubfeatcreators.domain.match.VectorRepository;
import com.hubfeatcreators.domain.social.SocialAccount;
import com.hubfeatcreators.domain.social.SocialAccountRepository;
import com.hubfeatcreators.domain.social.SocialPost;
import com.hubfeatcreators.domain.social.SocialPostRepository;
import com.hubfeatcreators.domain.social.SocialSnapshot;
import com.hubfeatcreators.domain.social.SocialSnapshotRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CreatorFeatureBuilderTest {

    @Mock SocialAccountRepository accountRepo;
    @Mock SocialSnapshotRepository snapshotRepo;
    @Mock SocialPostRepository postRepo;
    @Mock CreatorProfileFeatureRepository featureRepo;
    @Mock EmbeddingService embeddingService;
    @Mock VectorRepository vectorRepo;

    @InjectMocks CreatorFeatureBuilder builder;

    UUID influenciadorId = UUID.randomUUID();

    @Test
    void buildAndSave_no_accounts_creates_feature_with_zero_metrics() {
        when(accountRepo.findByInfluenciadorId(influenciadorId)).thenReturn(List.of());
        when(featureRepo.findByInfluenciadorId(influenciadorId)).thenReturn(Optional.empty());
        when(featureRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(embeddingService.embed(any())).thenReturn(new float[384]);

        CreatorProfileFeature result = builder.buildAndSave(influenciadorId);

        assertThat(result.getEngagement30d()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getFreqPost30d()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(vectorRepo).upsertCreatorEmbedding(eq(influenciadorId), any());
    }

    @Test
    void buildAndSave_with_snapshots_computes_average_engagement() {
        SocialAccount account = buildAccount();
        UUID accountId = account.getId();

        SocialSnapshot s1 =
                new SocialSnapshot(
                        accountId, LocalDate.now(), 1000, 100, 50, BigDecimal.valueOf(4.0), null);
        SocialSnapshot s2 =
                new SocialSnapshot(
                        accountId,
                        LocalDate.now().minusDays(1),
                        1000,
                        100,
                        50,
                        BigDecimal.valueOf(6.0),
                        null);

        when(accountRepo.findByInfluenciadorId(influenciadorId)).thenReturn(List.of(account));
        when(snapshotRepo.findBySocialAccountIdOrderByDiaDesc(eq(accountId), any(Pageable.class)))
                .thenReturn(List.of(s1, s2));
        when(postRepo.findTop30BySocialAccountIdOrderByPostedAtDesc(accountId))
                .thenReturn(List.of());
        when(featureRepo.findByInfluenciadorId(influenciadorId)).thenReturn(Optional.empty());
        when(featureRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(embeddingService.embed(any())).thenReturn(new float[384]);

        CreatorProfileFeature result = builder.buildAndSave(influenciadorId);

        // avg of (4.0 + 6.0) / 1 account = 10.0
        assertThat(result.getEngagement30d()).isEqualByComparingTo(BigDecimal.valueOf(10.0));
    }

    @Test
    void buildAndSave_counts_posts_for_freq() {
        SocialAccount account = buildAccount();
        UUID accountId = account.getId();

        List<SocialPost> posts =
                List.of(mock(SocialPost.class), mock(SocialPost.class), mock(SocialPost.class));

        when(accountRepo.findByInfluenciadorId(influenciadorId)).thenReturn(List.of(account));
        when(snapshotRepo.findBySocialAccountIdOrderByDiaDesc(any(), any())).thenReturn(List.of());
        when(postRepo.findTop30BySocialAccountIdOrderByPostedAtDesc(accountId)).thenReturn(posts);
        when(featureRepo.findByInfluenciadorId(influenciadorId)).thenReturn(Optional.empty());
        when(featureRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(embeddingService.embed(any())).thenReturn(new float[384]);

        CreatorProfileFeature result = builder.buildAndSave(influenciadorId);

        // 3 posts / 1 account = 3.00
        assertThat(result.getFreqPost30d()).isEqualByComparingTo(BigDecimal.valueOf(3.0));
    }

    @Test
    void buildAndSave_updates_existing_feature_rather_than_creating_new() {
        CreatorProfileFeature existing = new CreatorProfileFeature(influenciadorId);

        when(accountRepo.findByInfluenciadorId(influenciadorId)).thenReturn(List.of());
        when(featureRepo.findByInfluenciadorId(influenciadorId)).thenReturn(Optional.of(existing));
        when(featureRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(embeddingService.embed(any())).thenReturn(new float[384]);

        CreatorProfileFeature result = builder.buildAndSave(influenciadorId);

        assertThat(result).isSameAs(existing);
        // Only one save — no new entity created
        verify(featureRepo, times(1)).save(existing);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private SocialAccount buildAccount() {
        SocialAccount a =
                new SocialAccount(
                        influenciadorId,
                        "INSTAGRAM",
                        "ext-001",
                        "@creator",
                        new byte[16],
                        new byte[12],
                        new byte[16],
                        new byte[12],
                        null);
        // Set id via reflection (no JPA in unit tests)
        try {
            var f = SocialAccount.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(a, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return a;
    }
}
