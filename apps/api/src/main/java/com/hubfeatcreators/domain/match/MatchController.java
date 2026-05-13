package com.hubfeatcreators.domain.match;

import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/match")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping("/run")
    @RequirePermission("MTCW")
    public ResponseEntity<List<SugestaoDto>> runMatch(
            @AuthenticationPrincipal AuthPrincipal principal, @RequestParam UUID prospeccaoId) {
        List<MatchSugestao> sugestoes =
                matchService.runMatch(principal.assessoriaId(), prospeccaoId);
        return ResponseEntity.ok(sugestoes.stream().map(SugestaoDto::from).toList());
    }

    @GetMapping
    @RequirePermission("MTCR")
    public ResponseEntity<List<SugestaoDto>> getSugestoes(
            @AuthenticationPrincipal AuthPrincipal principal, @RequestParam UUID prospeccaoId) {
        List<MatchSugestao> sugestoes =
                matchService.getSugestoes(principal.assessoriaId(), prospeccaoId);
        return ResponseEntity.ok(sugestoes.stream().map(SugestaoDto::from).toList());
    }

    @GetMapping("/reverse")
    @RequirePermission("MTCR")
    public ResponseEntity<List<SugestaoDto>> getReverse(
            @AuthenticationPrincipal AuthPrincipal principal, @RequestParam UUID influenciadorId) {
        List<MatchSugestao> sugestoes =
                matchService.getReverse(principal.assessoriaId(), influenciadorId);
        return ResponseEntity.ok(sugestoes.stream().map(SugestaoDto::from).toList());
    }

    @PostMapping("/feedback")
    @RequirePermission("MTCW")
    public ResponseEntity<FeedbackDto> addFeedback(
            @AuthenticationPrincipal AuthPrincipal principal, @RequestBody FeedbackRequest req) {
        MatchFeedback fb =
                matchService.addFeedback(
                        principal.assessoriaId(),
                        req.sugestaoId(),
                        principal.usuarioId(),
                        req.sinal(),
                        req.comentario());
        return ResponseEntity.ok(FeedbackDto.from(fb));
    }

    @PostMapping("/optout/{influenciadorId}")
    @RequirePermission("MTCW")
    public ResponseEntity<Void> optout(
            @PathVariable UUID influenciadorId, @RequestParam(required = false) String motivo) {
        matchService.optout(influenciadorId, motivo);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/optout/{influenciadorId}")
    @RequirePermission("MTCW")
    public ResponseEntity<Void> optin(@PathVariable UUID influenciadorId) {
        matchService.optin(influenciadorId);
        return ResponseEntity.noContent().build();
    }

    record SugestaoDto(
            UUID id,
            UUID prospeccaoId,
            UUID influenciadorId,
            BigDecimal score,
            List<Map<String, Object>> razoes,
            String modeloVersao,
            Instant geradoEm) {
        static SugestaoDto from(MatchSugestao s) {
            return new SugestaoDto(
                    s.getId(),
                    s.getProspeccaoId(),
                    s.getInfluenciadorId(),
                    s.getScore(),
                    s.getRazoes(),
                    s.getModeloVersao(),
                    s.getGeradoEm());
        }
    }

    record FeedbackRequest(UUID sugestaoId, String sinal, String comentario) {}

    record FeedbackDto(
            UUID id, UUID sugestaoId, String sinal, String comentario, Instant createdAt) {
        static FeedbackDto from(MatchFeedback fb) {
            return new FeedbackDto(
                    fb.getId(),
                    fb.getSugestaoId(),
                    fb.getSinal(),
                    fb.getComentario(),
                    fb.getCreatedAt());
        }
    }
}
