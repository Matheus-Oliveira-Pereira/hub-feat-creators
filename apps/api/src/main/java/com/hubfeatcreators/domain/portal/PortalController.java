package com.hubfeatcreators.domain.portal;

import com.hubfeatcreators.domain.tarefa.Tarefa;
import com.hubfeatcreators.domain.tarefa.TarefaComentario;
import com.hubfeatcreators.infra.security.CreatorPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/portal/me")
@PreAuthorize("hasRole('CREATOR')")
public class PortalController {

    private final PortalService portalService;

    public PortalController(PortalService portalService) {
        this.portalService = portalService;
    }

    // ─── Tarefas ──────────────────────────────────────────────────────────────

    @GetMapping("/tarefas")
    public List<Tarefa> listarTarefas(@AuthenticationPrincipal CreatorPrincipal principal) {
        return portalService.listarTarefas(principal.assessoriaId(), principal.influenciadorId());
    }

    @GetMapping("/tarefas/{tarefaId}")
    public Tarefa detalharTarefa(
            @AuthenticationPrincipal CreatorPrincipal principal,
            @PathVariable UUID tarefaId) {
        return portalService.detalharTarefa(principal.assessoriaId(), principal.influenciadorId(), tarefaId);
    }

    // ─── Comentários ──────────────────────────────────────────────────────────

    @GetMapping("/tarefas/{tarefaId}/comentarios")
    public List<TarefaComentario> listarComentarios(
            @AuthenticationPrincipal CreatorPrincipal principal,
            @PathVariable UUID tarefaId) {
        return portalService.listarComentarios(principal.assessoriaId(), principal.influenciadorId(), tarefaId);
    }

    @PostMapping("/tarefas/{tarefaId}/comentarios")
    @ResponseStatus(HttpStatus.CREATED)
    public TarefaComentario comentar(
            @AuthenticationPrincipal CreatorPrincipal principal,
            @PathVariable UUID tarefaId,
            @Valid @RequestBody ComentarioRequest req) {
        return portalService.comentar(
                principal.assessoriaId(), principal.influenciadorId(),
                tarefaId, principal.creatorUserId(), req.texto());
    }

    // ─── Entregáveis ──────────────────────────────────────────────────────────

    @GetMapping("/tarefas/{tarefaId}/entregaveis")
    public List<CreatorEntregavel> listarEntregaveis(
            @AuthenticationPrincipal CreatorPrincipal principal,
            @PathVariable UUID tarefaId) {
        return portalService.listarEntregaveis(principal.assessoriaId(), principal.influenciadorId(), tarefaId);
    }

    @PostMapping("/tarefas/{tarefaId}/entregaveis")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatorEntregavel enviarEntregavel(
            @AuthenticationPrincipal CreatorPrincipal principal,
            @PathVariable UUID tarefaId,
            @RequestParam("file") MultipartFile file) {
        return portalService.enviarEntregavel(
                principal.assessoriaId(), principal.influenciadorId(),
                tarefaId, principal.creatorUserId(), file);
    }

    @GetMapping("/entregaveis/{entregavelId}/download")
    public ResponseEntity<InputStreamResource> download(
            @AuthenticationPrincipal CreatorPrincipal principal,
            @PathVariable UUID entregavelId) {
        PortalService.DownloadResult dl = portalService.downloadEntregavel(
                principal.assessoriaId(), entregavelId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + dl.filename() + "\"")
                .contentType(MediaType.parseMediaType(
                        dl.contentType() != null ? dl.contentType() : "application/octet-stream"))
                .contentLength(dl.sizeBytes())
                .body(new InputStreamResource(dl.stream()));
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    public record ComentarioRequest(@NotBlank String texto) {}
}
