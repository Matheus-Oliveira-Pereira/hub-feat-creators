package com.hubfeatcreators.domain.social;

import com.hubfeatcreators.infra.job.JobService;
import com.hubfeatcreators.infra.social.InstagramApiClient;
import com.hubfeatcreators.infra.social.SocialApiException;
import com.hubfeatcreators.infra.social.TiktokApiClient;
import com.hubfeatcreators.infra.social.YoutubeApiClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SocialService {

    private static final Logger log = LoggerFactory.getLogger(SocialService.class);

    private final SocialAccountRepository accountRepo;
    private final SocialSnapshotRepository snapshotRepo;
    private final SocialPostRepository postRepo;
    private final SocialConsentimentoRepository consentimentoRepo;
    private final QuotaUsoRepository quotaRepo;
    private final SocialCipherService cipher;
    private final JobService jobService;
    private final InstagramApiClient instagramClient;
    private final YoutubeApiClient youtubeClient;
    private final TiktokApiClient tiktokClient;

    public SocialService(
            SocialAccountRepository accountRepo,
            SocialSnapshotRepository snapshotRepo,
            SocialPostRepository postRepo,
            SocialConsentimentoRepository consentimentoRepo,
            QuotaUsoRepository quotaRepo,
            SocialCipherService cipher,
            JobService jobService,
            InstagramApiClient instagramClient,
            YoutubeApiClient youtubeClient,
            TiktokApiClient tiktokClient) {
        this.accountRepo = accountRepo;
        this.snapshotRepo = snapshotRepo;
        this.postRepo = postRepo;
        this.consentimentoRepo = consentimentoRepo;
        this.quotaRepo = quotaRepo;
        this.cipher = cipher;
        this.jobService = jobService;
        this.instagramClient = instagramClient;
        this.youtubeClient = youtubeClient;
        this.tiktokClient = tiktokClient;
    }

    @Transactional
    public SocialAccount connectInstagram(UUID influenciadorId, String code) {
        InstagramApiClient.TokenResponse token = instagramClient.exchangeCode(code);
        Map<String, Object> profile = instagramClient.getUserProfile(token.accessToken());

        String handle = (String) profile.getOrDefault("username", "");
        Instant expiresAt = Instant.now().plusSeconds(token.expiresIn());

        SocialCipherService.Encrypted enc = cipher.encrypt(token.accessToken());
        SocialAccount account =
                accountRepo
                        .findByPlataformaAndExternalUserId("INSTAGRAM", token.userId())
                        .orElse(null);

        if (account == null) {
            account =
                    new SocialAccount(
                            influenciadorId,
                            "INSTAGRAM",
                            token.userId(),
                            handle,
                            enc.ciphertext(),
                            enc.nonce(),
                            null,
                            null,
                            expiresAt);
        } else {
            account.setHandle(handle);
            account.setAccessTokenEnc(enc.ciphertext());
            account.setTokenNonce(enc.nonce());
            account.setExpiresAt(expiresAt);
            account.setStatus("ATIVA");
        }
        account = accountRepo.save(account);
        gravarConsentimento(influenciadorId, "INSTAGRAM", token.userId());
        enqueueSync(account.getId());
        return account;
    }

    @Transactional
    public SocialAccount connectYoutube(UUID influenciadorId, String code) {
        YoutubeApiClient.TokenResponse token = youtubeClient.exchangeCode(code);
        Map<String, Object> stats = youtubeClient.getChannelStats(token.accessToken());

        String channelId = extractYoutubeChannelId(stats);
        String handle = extractYoutubeHandle(stats);
        Instant expiresAt = Instant.now().plusSeconds(token.expiresIn());

        SocialCipherService.Encrypted encAccess = cipher.encrypt(token.accessToken());
        byte[] refreshEnc = null, refreshNonce = null;
        if (token.refreshToken() != null) {
            SocialCipherService.Encrypted encRefresh = cipher.encrypt(token.refreshToken());
            refreshEnc = encRefresh.ciphertext();
            refreshNonce = encRefresh.nonce();
        }

        SocialAccount account =
                accountRepo.findByPlataformaAndExternalUserId("YOUTUBE", channelId).orElse(null);
        if (account == null) {
            account =
                    new SocialAccount(
                            influenciadorId,
                            "YOUTUBE",
                            channelId,
                            handle,
                            encAccess.ciphertext(),
                            encAccess.nonce(),
                            refreshEnc,
                            refreshNonce,
                            expiresAt);
        } else {
            account.setHandle(handle);
            account.setAccessTokenEnc(encAccess.ciphertext());
            account.setTokenNonce(encAccess.nonce());
            account.setRefreshTokenEnc(refreshEnc);
            account.setRefreshNonce(refreshNonce);
            account.setExpiresAt(expiresAt);
            account.setStatus("ATIVA");
        }
        account = accountRepo.save(account);
        gravarConsentimento(influenciadorId, "YOUTUBE", channelId);
        enqueueSync(account.getId());
        return account;
    }

    @Transactional
    public SocialAccount connectTiktok(UUID influenciadorId, String code) {
        TiktokApiClient.TokenResponse token = tiktokClient.exchangeCode(code);
        Map<String, Object> userInfo = tiktokClient.getUserInfo(token.accessToken());

        String handle = extractTiktokHandle(userInfo);
        Instant expiresAt = Instant.now().plusSeconds(token.expiresIn());

        SocialCipherService.Encrypted encAccess = cipher.encrypt(token.accessToken());
        byte[] refreshEnc = null, refreshNonce = null;
        if (token.refreshToken() != null) {
            SocialCipherService.Encrypted encRefresh = cipher.encrypt(token.refreshToken());
            refreshEnc = encRefresh.ciphertext();
            refreshNonce = encRefresh.nonce();
        }

        SocialAccount account =
                accountRepo
                        .findByPlataformaAndExternalUserId("TIKTOK", token.openId())
                        .orElse(null);
        if (account == null) {
            account =
                    new SocialAccount(
                            influenciadorId,
                            "TIKTOK",
                            token.openId(),
                            handle,
                            encAccess.ciphertext(),
                            encAccess.nonce(),
                            refreshEnc,
                            refreshNonce,
                            expiresAt);
        } else {
            account.setHandle(handle);
            account.setAccessTokenEnc(encAccess.ciphertext());
            account.setTokenNonce(encAccess.nonce());
            account.setRefreshTokenEnc(refreshEnc);
            account.setRefreshNonce(refreshNonce);
            account.setExpiresAt(expiresAt);
            account.setStatus("ATIVA");
        }
        account = accountRepo.save(account);
        gravarConsentimento(influenciadorId, "TIKTOK", token.openId());
        enqueueSync(account.getId());
        return account;
    }

    @Transactional
    public void disconnect(UUID accountId) {
        SocialAccount account =
                accountRepo
                        .findById(accountId)
                        .orElseThrow(
                                () ->
                                        com.hubfeatcreators.infra.web.BusinessException.notFound(
                                                "SOCIAL_ACCOUNT"));

        // best-effort upstream revoke — log and continue on failure
        try {
            revokeUpstream(account);
        } catch (Exception e) {
            log.warn("Upstream revoke failed for account {}: {}", accountId, e.getMessage());
        }

        account.revogar();
        accountRepo.save(account);

        consentimentoRepo
                .findTopByInfluenciadorIdAndPlataformaAndRevogadoEmIsNullOrderByDadoEmDesc(
                        account.getInfluenciadorId(), account.getPlataforma())
                .ifPresent(
                        c -> {
                            c.revogar();
                            consentimentoRepo.save(c);
                        });
    }

    @Transactional
    public void refreshToken(UUID accountId) {
        SocialAccount account = accountRepo.findById(accountId).orElseThrow();
        if (account.getRefreshTokenEnc() == null) {
            account.setStatus("TOKEN_EXPIRADO");
            accountRepo.save(account);
            return;
        }
        try {
            String refreshToken =
                    cipher.decrypt(account.getRefreshTokenEnc(), account.getRefreshNonce());
            switch (account.getPlataforma()) {
                case "YOUTUBE" -> {
                    YoutubeApiClient.TokenResponse t = youtubeClient.refreshToken(refreshToken);
                    SocialCipherService.Encrypted enc = cipher.encrypt(t.accessToken());
                    account.setAccessTokenEnc(enc.ciphertext());
                    account.setTokenNonce(enc.nonce());
                    account.setExpiresAt(Instant.now().plusSeconds(t.expiresIn()));
                    account.setStatus("ATIVA");
                }
                case "TIKTOK" -> {
                    TiktokApiClient.TokenResponse t = tiktokClient.refreshToken(refreshToken);
                    SocialCipherService.Encrypted enc = cipher.encrypt(t.accessToken());
                    account.setAccessTokenEnc(enc.ciphertext());
                    account.setTokenNonce(enc.nonce());
                    account.setExpiresAt(Instant.now().plusSeconds(t.expiresIn()));
                    account.setStatus("ATIVA");
                }
                default -> account.setStatus("TOKEN_EXPIRADO");
            }
        } catch (SocialApiException e) {
            log.warn(
                    "Token refresh failed for account {}: {} {}",
                    accountId,
                    e.getStatusCode(),
                    e.getMessage());
            account.setStatus("TOKEN_EXPIRADO");
        }
        accountRepo.save(account);
    }

    @Transactional
    public void syncAccount(UUID accountId) {
        SocialAccount account = accountRepo.findById(accountId).orElseThrow();
        if (!"ATIVA".equals(account.getStatus())) return;
        try {
            String accessToken =
                    cipher.decrypt(account.getAccessTokenEnc(), account.getTokenNonce());
            switch (account.getPlataforma()) {
                case "INSTAGRAM" -> syncInstagram(account, accessToken);
                case "YOUTUBE" -> syncYoutube(account, accessToken);
                case "TIKTOK" -> syncTiktok(account, accessToken);
            }
            account.setUltimoSyncEm(Instant.now());
            accountRepo.save(account);
        } catch (SocialApiException e) {
            log.warn(
                    "Sync failed for account {}: {} {}",
                    accountId,
                    e.getStatusCode(),
                    e.getMessage());
            if (e.getStatusCode() == 401 || e.getStatusCode() == 400) {
                account.setStatus("TOKEN_EXPIRADO");
                accountRepo.save(account);
            }
        }
    }

    public List<SocialAccount> listByInfluenciador(UUID influenciadorId) {
        return accountRepo.findByInfluenciadorId(influenciadorId);
    }

    public List<SocialSnapshot> listSnapshots(UUID accountId, int limit) {
        SocialAccount account =
                accountRepo
                        .findById(accountId)
                        .orElseThrow(
                                () ->
                                        com.hubfeatcreators.infra.web.BusinessException.notFound(
                                                "SOCIAL_ACCOUNT"));
        return snapshotRepo.findBySocialAccountIdOrderByDiaDesc(
                account.getId(),
                org.springframework.data.domain.PageRequest.of(0, Math.min(limit, 90)));
    }

    // --- private helpers ---

    private void syncInstagram(SocialAccount account, String accessToken) {
        Map<String, Object> profile = instagramClient.getUserProfile(accessToken);
        Integer followers = toInt(profile.get("followers_count"));
        Integer following = toInt(profile.get("follows_count"));
        Integer postsTotal = toInt(profile.get("media_count"));

        upsertSnapshot(account.getId(), followers, following, postsTotal, null, profile);

        Map<String, Object> media = instagramClient.getRecentMedia(accessToken);
        if (media.containsKey("data") && media.get("data") instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> m) upsertInstagramPost(account.getId(), m);
            }
        }
        trackQuota("INSTAGRAM", 2);
    }

    private void syncYoutube(SocialAccount account, String accessToken) {
        Map<String, Object> stats = youtubeClient.getChannelStats(accessToken);
        if (stats.containsKey("items")
                && stats.get("items") instanceof List<?> items
                && !items.isEmpty()) {
            Object item = items.get(0);
            if (item instanceof Map<?, ?> ch) {
                Map<?, ?> st = (Map<?, ?>) ((Map<?, ?>) ch).get("statistics");
                Integer followers = st != null ? toInt(st.get("subscriberCount")) : null;
                Integer postsTotal = st != null ? toInt(st.get("videoCount")) : null;
                upsertSnapshot(account.getId(), followers, null, postsTotal, null, stats);
            }
        }
        trackQuota("YOUTUBE", 3);
    }

    private void syncTiktok(SocialAccount account, String accessToken) {
        Map<String, Object> info = tiktokClient.getUserInfo(accessToken);
        if (info.containsKey("data") && info.get("data") instanceof Map<?, ?> data) {
            if (data.get("user") instanceof Map<?, ?> user) {
                Integer followers = toInt(user.get("follower_count"));
                Integer following = toInt(user.get("following_count"));
                Integer postsTotal = toInt(user.get("video_count"));
                upsertSnapshot(account.getId(), followers, following, postsTotal, null, info);
            }
        }
        trackQuota("TIKTOK", 1);
    }

    private void upsertSnapshot(
            UUID accountId,
            Integer followers,
            Integer following,
            Integer postsTotal,
            BigDecimal engagementRate,
            Map<String, Object> payload) {
        LocalDate today = LocalDate.now();
        snapshotRepo
                .findBySocialAccountIdAndDia(accountId, today)
                .ifPresentOrElse(
                        s ->
                                log.debug(
                                        "Snapshot already exists for account {} on {}",
                                        accountId,
                                        today),
                        () ->
                                snapshotRepo.save(
                                        new SocialSnapshot(
                                                accountId,
                                                today,
                                                followers,
                                                following,
                                                postsTotal,
                                                engagementRate,
                                                toStringMap(payload))));
    }

    @SuppressWarnings("unchecked")
    private void upsertInstagramPost(UUID accountId, Map<?, ?> m) {
        String externalId = (String) m.get("id");
        if (externalId == null) return;
        int likes = toInt(m.get("like_count"));
        int comments = toInt(m.get("comments_count"));
        postRepo.findBySocialAccountIdAndExternalPostId(accountId, externalId)
                .ifPresentOrElse(
                        p -> {
                            p.update(likes, comments, 0, 0);
                            postRepo.save(p);
                        },
                        () ->
                                postRepo.save(
                                        new SocialPost(
                                                accountId,
                                                externalId,
                                                parseInstant(m.get("timestamp")),
                                                (String) m.get("media_type"),
                                                (String) m.get("caption"),
                                                likes,
                                                comments,
                                                0,
                                                0,
                                                (String) m.get("permalink"),
                                                toStringMap((Map<String, Object>) m))));
    }

    private void gravarConsentimento(UUID influenciadorId, String plataforma, String userId) {
        consentimentoRepo.save(
                new SocialConsentimento(
                        influenciadorId,
                        plataforma,
                        Map.of("externalUserId", userId, "ts", Instant.now().toString())));
    }

    private void enqueueSync(UUID accountId) {
        jobService.enqueue(
                "SOCIAL_SYNC",
                Map.of("accountId", accountId.toString()),
                UUID.nameUUIDFromBytes(
                        ("SOCIAL_SYNC:" + accountId + ":" + LocalDate.now()).getBytes()));
    }

    private void revokeUpstream(SocialAccount account) {
        // best-effort — platforms don't always expose a revoke endpoint in basic tier
        log.info(
                "Upstream revoke requested for {} account {}",
                account.getPlataforma(),
                account.getId());
    }

    private void trackQuota(String plataforma, long units) {
        LocalDate today = LocalDate.now();
        QuotaUso quota =
                quotaRepo
                        .findById(new QuotaUso.Pk(plataforma, today))
                        .orElse(new QuotaUso(plataforma, today));
        quota.addUnits(units);
        quotaRepo.save(quota);
    }

    private static Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Instant parseInstant(Object v) {
        if (v == null) return null;
        try {
            return Instant.parse(v.toString());
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toStringMap(Map<?, ?> m) {
        return (Map<String, Object>) m;
    }

    private String extractYoutubeChannelId(Map<String, Object> stats) {
        if (stats.get("items") instanceof List<?> items
                && !items.isEmpty()
                && items.get(0) instanceof Map<?, ?> ch) {
            return (String) ((Map<?, ?>) ch).get("id");
        }
        return "unknown";
    }

    private String extractYoutubeHandle(Map<String, Object> stats) {
        if (stats.get("items") instanceof List<?> items
                && !items.isEmpty()
                && items.get(0) instanceof Map<?, ?> ch
                && ch.get("snippet") instanceof Map<?, ?> sn) {
            return (String) sn.get("title");
        }
        return null;
    }

    private String extractTiktokHandle(Map<String, Object> info) {
        if (info.get("data") instanceof Map<?, ?> d && d.get("user") instanceof Map<?, ?> u) {
            return (String) u.get("display_name");
        }
        return null;
    }
}
