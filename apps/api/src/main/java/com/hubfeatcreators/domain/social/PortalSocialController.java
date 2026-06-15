package com.hubfeatcreators.domain.social;

import com.hubfeatcreators.infra.security.CreatorPrincipal;
import com.hubfeatcreators.infra.social.InstagramApiClient;
import com.hubfeatcreators.infra.social.TiktokApiClient;
import com.hubfeatcreators.infra.social.YoutubeApiClient;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portal/me/social")
@PreAuthorize("hasRole('CREATOR')")
public class PortalSocialController {

    private final SocialService socialService;
    private final OAuthStateStore stateStore;
    private final InstagramApiClient instagramClient;
    private final YoutubeApiClient youtubeClient;
    private final TiktokApiClient tiktokClient;

    public PortalSocialController(
            SocialService socialService,
            OAuthStateStore stateStore,
            InstagramApiClient instagramClient,
            YoutubeApiClient youtubeClient,
            TiktokApiClient tiktokClient) {
        this.socialService = socialService;
        this.stateStore = stateStore;
        this.instagramClient = instagramClient;
        this.youtubeClient = youtubeClient;
        this.tiktokClient = tiktokClient;
    }

    @GetMapping
    public List<SocialSnapshotController.SocialAccountDto> listMineAccounts(
            @AuthenticationPrincipal CreatorPrincipal creator) {
        return socialService
                .listByInfluenciador(creator.influenciadorId())
                .stream()
                .map(SocialSnapshotController.SocialAccountDto::from)
                .toList();
    }

    @GetMapping("/auth/{plataforma}/start")
    public ResponseEntity<Void> startOAuth(
            @PathVariable String plataforma, @AuthenticationPrincipal CreatorPrincipal creator) {
        String state = stateStore.generate(creator.influenciadorId());
        String url =
                switch (plataforma.toUpperCase()) {
                    case "INSTAGRAM" -> instagramClient.buildAuthUrl(state);
                    case "YOUTUBE" -> youtubeClient.buildAuthUrl(state);
                    case "TIKTOK" -> tiktokClient.buildAuthUrl(state);
                    default ->
                            throw com.hubfeatcreators.infra.web.BusinessException.badRequest(
                                    "PLATAFORMA_INVALIDA", "Plataforma não suportada");
                };
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> disconnect(
            @PathVariable UUID id, @AuthenticationPrincipal CreatorPrincipal creator) {
        socialService.disconnect(id);
        return ResponseEntity.noContent().build();
    }
}
