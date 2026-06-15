package com.hubfeatcreators.domain.social;

import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/social")
public class SocialSnapshotController {

    private final SocialService socialService;

    public SocialSnapshotController(SocialService socialService) {
        this.socialService = socialService;
    }

    @GetMapping("/accounts")
    @RequirePermission("BINF")
    public List<SocialAccountDto> listAccounts(@RequestParam UUID influenciadorId) {
        return socialService.listByInfluenciador(influenciadorId).stream()
                .map(SocialAccountDto::from)
                .toList();
    }

    @GetMapping("/snapshots")
    @RequirePermission("BINF")
    public List<SocialSnapshotDto> listSnapshots(
            @RequestParam UUID accountId, @RequestParam(defaultValue = "30") int limit) {
        return socialService.listSnapshots(accountId, limit).stream()
                .map(SocialSnapshotDto::from)
                .toList();
    }

    public record SocialAccountDto(
            UUID id,
            String plataforma,
            String handle,
            String status,
            java.time.Instant conectadoEm,
            java.time.Instant ultimoSyncEm) {

        public static SocialAccountDto from(SocialAccount a) {
            return new SocialAccountDto(
                    a.getId(),
                    a.getPlataforma(),
                    a.getHandle(),
                    a.getStatus(),
                    a.getConectadoEm(),
                    a.getUltimoSyncEm());
        }
    }

    public record SocialSnapshotDto(
            java.time.LocalDate dia,
            Integer followers,
            Integer following,
            Integer postsTotal,
            java.math.BigDecimal engagementRate) {

        public static SocialSnapshotDto from(SocialSnapshot s) {
            return new SocialSnapshotDto(
                    s.getDia(),
                    s.getFollowers(),
                    s.getFollowing(),
                    s.getPostsTotal(),
                    s.getEngagementRate());
        }
    }
}
