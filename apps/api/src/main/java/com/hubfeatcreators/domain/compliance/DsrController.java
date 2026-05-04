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

    /** Titular submits DSR request via token received by e-mail. */
    @PostMapping("/execute/{token}")
    public ResponseEntity<DsrResponse> executar(@PathVariable String token) {
        DsrSolicitacao sol = dsrService.executarComToken(token);
        return ResponseEntity.ok(DsrResponse.from(sol));
    }

    /** Export titular data for ACESSO/PORTABILIDADE (token already consumed by /execute). */
    @GetMapping("/dados/{titularTipo}/{titularId}")
    public ResponseEntity<Map<String, Object>> dados(
            @PathVariable String titularTipo,
            @PathVariable UUID titularId) {
        Map<String, Object> dados = dsrService.exportarDadosTitular(titularTipo, titularId);
        return ResponseEntity.ok(dados);
    }

    public record DsrResponse(
            UUID id,
            String titularTipo,
            UUID titularId,
            String tipo,
            String status,
            String prazoLegalEm,
            String atendidoEm) {

        static DsrResponse from(DsrSolicitacao s) {
            return new DsrResponse(
                    s.getId(),
                    s.getTitularTipo(),
                    s.getTitularId(),
                    s.getTipo().name(),
                    s.getStatus().name(),
                    s.getPrazoLegalEm().toString(),
                    s.getAtendidoEm() != null ? s.getAtendidoEm().toString() : null);
        }
    }
}
