package com.hubfeatcreators.domain.notificacao;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notificacoes")
public class NotificacaoController {

    private final NotificacaoService service;

    public NotificacaoController(NotificacaoService service) {
        this.service = service;
    }

    @GetMapping
    @RequirePermission(PermissionCodes.B_NOT)
    public Page<Notificacao> listar(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(required = false) NotificacaoTipo tipo,
            @RequestParam(defaultValue = "false") boolean apenasNaoLidas,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listar(principal.assessoriaId(), principal.usuarioId(), tipo, apenasNaoLidas, page, size);
    }

    @GetMapping("/contagem")
    @RequirePermission(PermissionCodes.B_NOT)
    public Map<String, Long> contagem(@AuthenticationPrincipal AuthPrincipal principal) {
        long naoLidas = service.contarNaoLidas(principal.assessoriaId(), principal.usuarioId());
        return Map.of("naoLidas", naoLidas);
    }

    @PostMapping("/{id}/lida")
    @RequirePermission(PermissionCodes.B_NOT)
    public void marcarLida(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id) {
        service.marcarLida(principal.assessoriaId(), id, principal.usuarioId());
    }

    @PostMapping("/lidas")
    @RequirePermission(PermissionCodes.B_NOT)
    public void marcarTodasLidas(@AuthenticationPrincipal AuthPrincipal principal) {
        service.marcarTodasLidas(principal.assessoriaId(), principal.usuarioId());
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequirePermission(PermissionCodes.B_NOT)
    public SseEmitter stream(@AuthenticationPrincipal AuthPrincipal principal) {
        return service.createEmitter(principal.usuarioId());
    }

    @GetMapping("/prefs")
    @RequirePermission(PermissionCodes.B_NOT)
    public List<NotificacaoPreferencia> preferencias(@AuthenticationPrincipal AuthPrincipal principal) {
        return service.preferencias(principal.usuarioId());
    }

    @PutMapping("/prefs/{tipo}/{canal}")
    @RequirePermission(PermissionCodes.B_NOT)
    public NotificacaoPreferencia atualizarPreferencia(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable NotificacaoTipo tipo,
            @PathVariable NotificacaoCanal canal,
            @RequestBody Map<String, Boolean> body) {
        boolean habilitado = Boolean.TRUE.equals(body.get("habilitado"));
        return service.atualizarPreferencia(principal.usuarioId(), tipo, canal, habilitado);
    }
}
