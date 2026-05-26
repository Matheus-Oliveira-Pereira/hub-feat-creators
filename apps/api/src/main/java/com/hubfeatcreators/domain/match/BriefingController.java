package com.hubfeatcreators.domain.match;

import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/briefings")
public class BriefingController {

    private final BriefingService briefingService;

    public BriefingController(BriefingService briefingService) {
        this.briefingService = briefingService;
    }

    @PostMapping
    @RequirePermission("MTCW")
    public ResponseEntity<BriefingDto> upsert(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody BriefingRequest req) {
        Briefing briefing =
                briefingService.upsertByProspeccao(
                        req.prospeccaoId(),
                        req.vertical(),
                        req.objetivo(),
                        req.audiencia(),
                        req.formato(),
                        req.budgetMin(),
                        req.budgetMax(),
                        req.texto());
        return ResponseEntity.ok(BriefingDto.from(briefing));
    }

    @GetMapping("/by-prospeccao/{prospeccaoId}")
    @RequirePermission("MTCR")
    public ResponseEntity<BriefingDto> getByProspeccao(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID prospeccaoId) {
        Briefing briefing = briefingService.getByProspeccao(prospeccaoId);
        return ResponseEntity.ok(BriefingDto.from(briefing));
    }

    @GetMapping
    @RequirePermission("MTCR")
    public ResponseEntity<List<BriefingDto>> list(
            @AuthenticationPrincipal AuthPrincipal principal) {
        List<Briefing> briefings = briefingService.listAll();
        return ResponseEntity.ok(briefings.stream().map(BriefingDto::from).toList());
    }

    record BriefingRequest(
            @NotNull UUID prospeccaoId,
            String vertical,
            String objetivo,
            Map<String, Object> audiencia,
            String formato,
            Long budgetMin,
            Long budgetMax,
            String texto) {}

    record BriefingDto(
            UUID id,
            UUID prospeccaoId,
            String vertical,
            String objetivo,
            Map<String, Object> audiencia,
            String formato,
            Long budgetMin,
            Long budgetMax,
            String texto,
            Instant atualizadoEm) {
        static BriefingDto from(Briefing b) {
            return new BriefingDto(
                    b.getId(),
                    b.getProspeccaoId(),
                    b.getVertical(),
                    b.getObjetivo(),
                    b.getAudiencia(),
                    b.getFormato(),
                    b.getBudgetMin(),
                    b.getBudgetMax(),
                    b.getTexto(),
                    b.getAtualizadoEm());
        }
    }
}
