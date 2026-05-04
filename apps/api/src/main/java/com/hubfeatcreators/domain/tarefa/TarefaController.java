package com.hubfeatcreators.domain.tarefa;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import com.hubfeatcreators.infra.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tarefas")
public class TarefaController {

  private final TarefaService service;

  public TarefaController(TarefaService service) {
    this.service = service;
  }

  // ─── DTOs ───────────────────────────────────────────────────────────────

  public record TarefaResponse(
      UUID id,
      UUID assessoriaId,
      String titulo,
      String descricao,
      Instant prazo,
      TarefaPrioridade prioridade,
      TarefaStatus status,
      UUID responsavelId,
      UUID criadorId,
      EntidadeTipo entidadeTipo,
      UUID entidadeId,
      Instant concluidaEm,
      Instant createdAt,
      Instant updatedAt) {}

  public record TarefaRequest(
      @NotBlank String titulo,
      String descricao,
      @NotNull Instant prazo,
      TarefaPrioridade prioridade,
      UUID responsavelId,
      EntidadeTipo entidadeTipo,
      UUID entidadeId) {}

  public record TarefaUpdateRequest(
      String titulo,
      String descricao,
      Instant prazo,
      TarefaPrioridade prioridade,
      UUID responsavelId,
      EntidadeTipo entidadeTipo,
      UUID entidadeId) {}

  public record StatusRequest(@NotNull TarefaStatus status) {}

  public record ComentarioRequest(@NotBlank String texto) {}

  public record ComentarioResponse(UUID id, UUID tarefaId, UUID autorId, String texto, Instant createdAt) {}

  public record AlertaResponse(long count) {}

  public record PreferenciaRequest(boolean digestDiarioEnabled) {}

  public record PreferenciaResponse(UUID usuarioId, boolean digestDiarioEnabled) {}

  // ─── Read ───────────────────────────────────────────────────────────────

  @GetMapping
  @RequirePermission(PermissionCodes.B_TAR)
  public PageResponse<TarefaResponse> listar(
      @AuthenticationPrincipal AuthPrincipal principal,
      @RequestParam(required = false) TarefaStatus status,
      @RequestParam(required = false) TarefaPrioridade prioridade,
      @RequestParam(required = false) UUID responsavelId,
      @RequestParam(required = false) String prazoFiltro,
      @RequestParam(defaultValue = "false") boolean minhas,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    Page<Tarefa> result = service.listar(
        principal, status, prioridade, responsavelId, prazoFiltro, minhas, page, size);
    var data = result.map(this::toResponse).getContent();
    String cursor = result.hasNext() ? String.valueOf(page + 1) : null;
    return PageResponse.of(data, cursor, size);
  }

  @GetMapping("/me")
  @RequirePermission(PermissionCodes.B_TAR)
  public PageResponse<TarefaResponse> minhasTarefas(
      @AuthenticationPrincipal AuthPrincipal principal,
      @RequestParam(required = false) TarefaStatus status,
      @RequestParam(required = false) String prazoFiltro,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    Page<Tarefa> result = service.listar(principal, status, null, null, prazoFiltro, true, page, size);
    var data = result.map(this::toResponse).getContent();
    String cursor = result.hasNext() ? String.valueOf(page + 1) : null;
    return PageResponse.of(data, cursor, size);
  }

  @GetMapping("/alerta")
  @RequirePermission(PermissionCodes.B_TAR)
  public AlertaResponse alerta(@AuthenticationPrincipal AuthPrincipal principal) {
    return new AlertaResponse(service.contarAlerta(principal));
  }

  @GetMapping("/{id}")
  @RequirePermission(PermissionCodes.B_TAR)
  public TarefaResponse buscar(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
    return toResponse(service.buscar(principal, id));
  }

  // ─── Write ──────────────────────────────────────────────────────────────

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @RequirePermission(PermissionCodes.C_TAR)
  public TarefaResponse criar(
      @AuthenticationPrincipal AuthPrincipal principal,
      @Valid @RequestBody TarefaRequest req) {
    return toResponse(service.criar(
        principal, req.titulo(), req.descricao(), req.prazo(),
        req.prioridade(), req.responsavelId(), req.entidadeTipo(), req.entidadeId()));
  }

  @PatchMapping("/{id}")
  @RequirePermission(PermissionCodes.E_TAR)
  public TarefaResponse atualizar(
      @AuthenticationPrincipal AuthPrincipal principal,
      @PathVariable UUID id,
      @Valid @RequestBody TarefaUpdateRequest req) {
    return toResponse(service.atualizar(
        principal, id, req.titulo(), req.descricao(), req.prazo(),
        req.prioridade(), req.responsavelId(), req.entidadeTipo(), req.entidadeId()));
  }

  @PatchMapping("/{id}/status")
  @RequirePermission(PermissionCodes.E_TAR)
  public TarefaResponse mudarStatus(
      @AuthenticationPrincipal AuthPrincipal principal,
      @PathVariable UUID id,
      @Valid @RequestBody StatusRequest req) {
    return toResponse(service.mudarStatus(principal, id, req.status()));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RequirePermission(PermissionCodes.D_TAR)
  public void deletar(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
    service.deletar(principal, id);
  }

  // ─── Comentários ────────────────────────────────────────────────────────

  @GetMapping("/{id}/comentarios")
  @RequirePermission(PermissionCodes.B_TAR)
  public List<ComentarioResponse> comentarios(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
    return service.comentarios(principal, id).stream().map(this::toComentarioResponse).toList();
  }

  @PostMapping("/{id}/comentarios")
  @ResponseStatus(HttpStatus.CREATED)
  @RequirePermission(PermissionCodes.C_TAR)
  public ComentarioResponse adicionarComentario(
      @AuthenticationPrincipal AuthPrincipal principal,
      @PathVariable UUID id,
      @Valid @RequestBody ComentarioRequest req) {
    return toComentarioResponse(service.adicionarComentario(principal, id, req.texto()));
  }

  // ─── Preferências ───────────────────────────────────────────────────────

  @GetMapping("/preferencias")
  @RequirePermission(PermissionCodes.B_TAR)
  public PreferenciaResponse preferencias(@AuthenticationPrincipal AuthPrincipal principal) {
    var pref = service.preferencias(principal);
    return new PreferenciaResponse(pref.getUsuarioId(), pref.isDigestDiarioEnabled());
  }

  @PatchMapping("/preferencias")
  @RequirePermission(PermissionCodes.B_TAR)
  public PreferenciaResponse atualizarPreferencias(
      @AuthenticationPrincipal AuthPrincipal principal,
      @RequestBody PreferenciaRequest req) {
    var pref = service.atualizarPreferencias(principal, req.digestDiarioEnabled());
    return new PreferenciaResponse(pref.getUsuarioId(), pref.isDigestDiarioEnabled());
  }

  // ─── Mappers ────────────────────────────────────────────────────────────

  private TarefaResponse toResponse(Tarefa t) {
    return new TarefaResponse(
        t.getId(), t.getAssessoriaId(), t.getTitulo(), t.getDescricao(),
        t.getPrazo(), t.getPrioridade(), t.getStatus(),
        t.getResponsavelId(), t.getCriadorId(),
        t.getEntidadeTipo(), t.getEntidadeId(),
        t.getConcluidaEm(), t.getCreatedAt(), t.getUpdatedAt());
  }

  private ComentarioResponse toComentarioResponse(TarefaComentario c) {
    return new ComentarioResponse(c.getId(), c.getTarefaId(), c.getAutorId(), c.getTexto(), c.getCreatedAt());
  }
}
