package com.hubfeatcreators.domain.historico;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import com.hubfeatcreators.infra.web.PageResponse;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/historico")
public class HistoricoController {

    private final EventoService eventoService;

    public HistoricoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping
    @RequirePermission(PermissionCodes.B_HIS)
    public PageResponse<Evento> listar(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(name = "entidade_tipo") String entidadeTipo,
            @RequestParam(name = "entidade_id") UUID entidadeId,
            @RequestParam(required = false) String tipos,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int size) {

        List<String> tiposList =
                tipos != null && !tipos.isBlank() ? Arrays.asList(tipos.split(",")) : null;
        int safeSize = Math.min(Math.max(1, size), 200);

        List<Evento> eventos =
                eventoService.listar(
                        principal, entidadeTipo, entidadeId, tiposList, cursor, safeSize);

        String nextCursor = null;
        if (eventos.size() == safeSize) {
            Evento last = eventos.get(eventos.size() - 1);
            nextCursor = eventoService.encodeCursor(last.getTs(), last.getId());
        }

        return PageResponse.of(eventos, nextCursor, safeSize);
    }
}
