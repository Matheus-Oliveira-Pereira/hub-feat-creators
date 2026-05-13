package com.hubfeatcreators.domain.social;

import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.CreatorPrincipal;
import com.hubfeatcreators.infra.social.InstagramApiClient;
import com.hubfeatcreators.infra.social.TiktokApiClient;
import com.hubfeatcreators.infra.social.YoutubeApiClient;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/social/auth")
public class SocialOAuthController {

    private final OAuthStateStore stateStore;
    private final SocialService socialService;
    private final InstagramApiClient instagramClient;
    private final YoutubeApiClient youtubeClient;
    private final TiktokApiClient tiktokClient;

    public SocialOAuthController(
            OAuthStateStore stateStore,
            SocialService socialService,
            InstagramApiClient instagramClient,
            YoutubeApiClient youtubeClient,
            TiktokApiClient tiktokClient) {
        this.stateStore = stateStore;
        this.socialService = socialService;
        this.instagramClient = instagramClient;
        this.youtubeClient = youtubeClient;
        this.tiktokClient = tiktokClient;
    }

    @GetMapping("/{plataforma}/start")
    public ResponseEntity<Void> start(
            @PathVariable String plataforma,
            @RequestParam UUID influenciadorId,
            @AuthenticationPrincipal Object principal) {
        UUID assessoriaId = resolveAssessoriaId(principal);
        String state = stateStore.generate(influenciadorId, assessoriaId);
        String url = buildAuthUrl(plataforma.toUpperCase(), state);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    @GetMapping("/{plataforma}/callback")
    public ResponseEntity<Void> callback(
            @PathVariable String plataforma,
            @RequestParam String code,
            @RequestParam String state) {
        OAuthStateStore.OAuthPending pending =
                stateStore
                        .consume(state)
                        .orElseThrow(
                                () ->
                                        com.hubfeatcreators.infra.web.BusinessException.badRequest(
                                                "OAUTH_STATE_INVALID",
                                                "OAuth state inválido ou expirado"));

        switch (plataforma.toUpperCase()) {
            case "INSTAGRAM" ->
                    socialService.connectInstagram(
                            pending.assessoriaId(), pending.influenciadorId(), code);
            case "YOUTUBE" ->
                    socialService.connectYoutube(
                            pending.assessoriaId(), pending.influenciadorId(), code);
            case "TIKTOK" ->
                    socialService.connectTiktok(
                            pending.assessoriaId(), pending.influenciadorId(), code);
            default ->
                    throw com.hubfeatcreators.infra.web.BusinessException.badRequest(
                            "PLATAFORMA_INVALIDA", "Plataforma não suportada");
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/influenciadores?social=connected"))
                .build();
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<Void> disconnect(
            @PathVariable UUID id, @AuthenticationPrincipal Object principal) {
        UUID assessoriaId = resolveAssessoriaId(principal);
        socialService.disconnect(assessoriaId, id);
        return ResponseEntity.noContent().build();
    }

    private String buildAuthUrl(String plataforma, String state) {
        return switch (plataforma) {
            case "INSTAGRAM" -> instagramClient.buildAuthUrl(state);
            case "YOUTUBE" -> youtubeClient.buildAuthUrl(state);
            case "TIKTOK" -> tiktokClient.buildAuthUrl(state);
            default ->
                    throw com.hubfeatcreators.infra.web.BusinessException.badRequest(
                            "PLATAFORMA_INVALIDA", "Plataforma não suportada");
        };
    }

    private UUID resolveAssessoriaId(Object principal) {
        if (principal instanceof AuthPrincipal ap) return ap.assessoriaId();
        if (principal instanceof CreatorPrincipal cp) return cp.assessoriaId();
        throw new org.springframework.security.access.AccessDeniedException("Unauthenticated");
    }
}
