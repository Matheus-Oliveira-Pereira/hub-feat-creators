package com.hubfeatcreators.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hubfeatcreators.domain.social.*;
import com.hubfeatcreators.infra.job.JobService;
import com.hubfeatcreators.infra.social.InstagramApiClient;
import com.hubfeatcreators.infra.social.SocialApiException;
import com.hubfeatcreators.infra.social.TiktokApiClient;
import com.hubfeatcreators.infra.social.YoutubeApiClient;
import com.hubfeatcreators.infra.web.BusinessException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SocialServiceTest {

    @Mock SocialAccountRepository accountRepo;
    @Mock SocialSnapshotRepository snapshotRepo;
    @Mock SocialPostRepository postRepo;
    @Mock SocialConsentimentoRepository consentimentoRepo;
    @Mock QuotaUsoRepository quotaRepo;
    @Mock SocialCipherService cipher;
    @Mock JobService jobService;
    @Mock InstagramApiClient instagramClient;
    @Mock YoutubeApiClient youtubeClient;
    @Mock TiktokApiClient tiktokClient;

    @InjectMocks SocialService service;

    UUID assessoriaId = UUID.randomUUID();
    UUID influenciadorId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        when(cipher.encrypt(any()))
                .thenReturn(
                        new SocialCipherService.Encrypted(
                                new byte[] {1, 2, 3}, new byte[] {4, 5, 6}));
    }

    @Test
    void connectInstagram_creates_new_account() {
        when(instagramClient.exchangeCode("code123"))
                .thenReturn(new InstagramApiClient.TokenResponse("tok", "ext-user-1", 5183944L));
        when(instagramClient.getUserProfile("tok"))
                .thenReturn(Map.of("username", "creator1", "followers_count", 1000));
        when(accountRepo.findByPlataformaAndExternalUserId("INSTAGRAM", "ext-user-1"))
                .thenReturn(Optional.empty());
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SocialAccount account = service.connectInstagram(assessoriaId, influenciadorId, "code123");

        assertThat(account.getPlataforma()).isEqualTo("INSTAGRAM");
        assertThat(account.getHandle()).isEqualTo("creator1");
        assertThat(account.getStatus()).isEqualTo("ATIVA");
        verify(jobService).enqueue(eq(assessoriaId), eq("SOCIAL_SYNC"), any(), any());
    }

    @Test
    void connectInstagram_updates_existing_account() {
        SocialAccount existing =
                new SocialAccount(
                        assessoriaId,
                        influenciadorId,
                        "INSTAGRAM",
                        "ext-user-1",
                        "old_handle",
                        new byte[] {1},
                        new byte[] {2},
                        null,
                        null,
                        Instant.now().plusSeconds(3600));
        when(instagramClient.exchangeCode("code456"))
                .thenReturn(new InstagramApiClient.TokenResponse("tok2", "ext-user-1", 5183944L));
        when(instagramClient.getUserProfile("tok2"))
                .thenReturn(Map.of("username", "creator_new", "followers_count", 2000));
        when(accountRepo.findByPlataformaAndExternalUserId("INSTAGRAM", "ext-user-1"))
                .thenReturn(Optional.of(existing));
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SocialAccount account = service.connectInstagram(assessoriaId, influenciadorId, "code456");

        assertThat(account.getHandle()).isEqualTo("creator_new");
        assertThat(account.getStatus()).isEqualTo("ATIVA");
    }

    @Test
    void disconnect_revokes_account_and_consentimento() {
        UUID accountId = UUID.randomUUID();
        SocialAccount account =
                new SocialAccount(
                        assessoriaId,
                        influenciadorId,
                        "INSTAGRAM",
                        "ext-1",
                        "handle",
                        new byte[] {1},
                        new byte[] {2},
                        null,
                        null,
                        Instant.now().plusSeconds(3600));
        when(accountRepo.findById(accountId)).thenReturn(Optional.of(account));
        when(consentimentoRepo
                        .findTopByInfluenciadorIdAndPlataformaAndRevogadoEmIsNullOrderByDadoEmDesc(
                                any(), any()))
                .thenReturn(Optional.empty());
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.disconnect(assessoriaId, accountId);

        assertThat(account.getStatus()).isEqualTo("REVOGADA");
        assertThat(account.getAccessTokenEnc()).isNull();
    }

    @Test
    void disconnect_throws_when_account_not_found() {
        UUID accountId = UUID.randomUUID();
        when(accountRepo.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.disconnect(assessoriaId, accountId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void refreshToken_marks_expired_on_api_error() {
        UUID accountId = UUID.randomUUID();
        SocialAccount account =
                new SocialAccount(
                        assessoriaId,
                        influenciadorId,
                        "YOUTUBE",
                        "ch-1",
                        "channel",
                        new byte[] {1},
                        new byte[] {2},
                        new byte[] {3},
                        new byte[] {4},
                        Instant.now().plusSeconds(3600));
        when(accountRepo.findById(accountId)).thenReturn(Optional.of(account));
        when(cipher.decrypt(any(), any())).thenReturn("refresh-token");
        when(youtubeClient.refreshToken("refresh-token"))
                .thenThrow(new SocialApiException("401", 401));
        when(accountRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.refreshToken(accountId);

        assertThat(account.getStatus()).isEqualTo("TOKEN_EXPIRADO");
    }

    @Test
    void syncAccount_skips_revoked_account() {
        UUID accountId = UUID.randomUUID();
        SocialAccount account =
                new SocialAccount(
                        assessoriaId,
                        influenciadorId,
                        "INSTAGRAM",
                        "ext-1",
                        "handle",
                        new byte[] {1},
                        new byte[] {2},
                        null,
                        null,
                        Instant.now());
        account.revogar();
        when(accountRepo.findById(accountId)).thenReturn(Optional.of(account));

        service.syncAccount(accountId);

        verifyNoInteractions(cipher, instagramClient);
    }
}
