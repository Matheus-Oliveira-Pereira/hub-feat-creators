package com.hubfeatcreators.domain.compliance;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/compliance")
public class ComplianceAdminController {

    private final DsrService dsrService;
    private final DataProcessingRecordRepository ropaRepo;

    public ComplianceAdminController(DsrService dsrService, DataProcessingRecordRepository ropaRepo) {
        this.dsrService = dsrService;
        this.ropaRepo = ropaRepo;
    }

    /** Create DSR on behalf of titular (OWNER/DPO initiates from support). */
    @PostMapping("/dsr")
    @RequirePermission(PermissionCodes.OWNR)
    public ResponseEntity<DsrTokenResponse> criarDsr(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody DsrRequest req) {
        var result = dsrService.criarSolicitacao(
                principal.assessoriaId(), req.titularTipo(), req.titularId(), req.tipo());
        return ResponseEntity.ok(new DsrTokenResponse(result.solicitacao().getId(), result.rawToken()));
    }

    /** List pending DSRs about to breach SLA (alerta 10d). */
    @GetMapping("/dsr/alertas")
    @RequirePermission(PermissionCodes.OWNR)
    public ResponseEntity<List<DsrAlertaResponse>> alertas() {
        var vencendo = dsrService.alertarVencendo(10);
        var response = vencendo.stream()
                .map(s -> new DsrAlertaResponse(s.getId(), s.getTipo().name(), s.getPrazoLegalEm().toString()))
                .toList();
        return ResponseEntity.ok(response);
    }

    /** ROPA — list active data processing records. */
    @GetMapping("/ropa")
    @RequirePermission(PermissionCodes.OWNR)
    public ResponseEntity<List<DataProcessingRecord>> ropa() {
        return ResponseEntity.ok(ropaRepo.findByVigenteTrue());
    }

    public record DsrRequest(String titularTipo, UUID titularId, DsrSolicitacao.TipoDsr tipo) {}
    public record DsrTokenResponse(UUID solicitacaoId, String token) {}
    public record DsrAlertaResponse(UUID id, String tipo, String prazoLegalEm) {}
}
