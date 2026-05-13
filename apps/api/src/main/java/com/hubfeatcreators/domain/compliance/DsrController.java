package com.hubfeatcreators.domain.compliance;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dsr")
public class DsrController {

    private final DsrService dsrService;

    public DsrController(DsrService dsrService) {
        this.dsrService = dsrService;
    }

    /**
     * Titular submits DSR request via token received by e-mail. For ACESSO/PORTABILIDADE the
     * response includes the exported data inline — no separate unauthenticated endpoint needed.
     */
    @PostMapping("/execute/{token}")
    public ResponseEntity<DsrResponse> executar(@PathVariable String token) {
        DsrService.DsrResultFull result = dsrService.executarComToken(token);
        return ResponseEntity.ok(DsrResponse.from(result));
    }

    public record DsrResponse(
            UUID id,
            String titularTipo,
            UUID titularId,
            String tipo,
            String status,
            String prazoLegalEm,
            String atendidoEm,
            Map<String, Object> dados) {

        static DsrResponse from(DsrService.DsrResultFull r) {
            DsrSolicitacao s = r.solicitacao();
            return new DsrResponse(
                    s.getId(),
                    s.getTitularTipo(),
                    s.getTitularId(),
                    s.getTipo().name(),
                    s.getStatus().name(),
                    s.getPrazoLegalEm().toString(),
                    s.getAtendidoEm() != null ? s.getAtendidoEm().toString() : null,
                    r.dados());
        }
    }
}
