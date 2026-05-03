package com.hubfeatcreators.domain.prospeccao;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import com.hubfeatcreators.infra.web.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/prospeccoes")
public class ProspeccaoController {

  private final ProspeccaoService service;

  public ProspeccaoController(ProspeccaoService service) {
    this.service = service;
  }

  public record ProspeccaoResponse(
      UUID id,
      UUID marcaId,
      UUID influenciadorId,
      UUID assessorResponsavelId,
      String titulo,
      ProspeccaoStatus status,
      Long valorEstimadoCentavos,
      String proximaAcao,
      LocalDate proximaAcaoEm,
      String observacoes,
      List<String> tags,
      MotivoPerda motivoPerda,
      String motivoPerdaDetalhe,
      Instant fechadaEm,
      Instant createdAt,
      Instant updatedAt,
      UUID createdBy) {}

  public record ProspeccaoRequest(
      @NotNull UUID marcaId,
      UUID influenciadorId,
      UUID assessorResponsavelId,
      @NotBlank String titulo,
      Long valorEstimadoCentavos,
      String proximaAcao,
      LocalDate proximaAcaoEm,
      String observacoes,
      String[] tags) {}

  public record StatusRequest(
      @NotNull ProspeccaoStatus status,
      MotivoPerda motivoPerda,
      String motivoPerdaDetalhe) {}

  public record ComentarioRequest(@NotBlank String texto) {}

  public record EventoResponse(
      UUID id, EventoTipo tipo, Object payload, UUID autorId, Instant createdAt) {}

  // ─── Read ───────────────────────────────────────────────────────────────
  @GetMapping
  @RequirePermission(PermissionCodes.B_PRO)
  public PageResponse<ProspeccaoResponse> listar(
      @AuthenticationPrincipal AuthPrincipal principal,
      @RequestParam(required = false) ProspeccaoStatus status,
      @RequestParam(required = false) UUID assessorId,
      @RequestParam(required = false) UUID marcaId,
      @RequestParam(required = false) String nome,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "30") int size) {
    Page<Prospeccao> result =
        service.listar(principal, status, assessorId, marcaId, nome, page, size);
    List<ProspeccaoResponse> data = result.map(this::toResponse).getContent();
    String cursor = result.hasNext() ? String.valueOf(page + 1) : null;
    return PageResponse.of(data, cursor, size);
  }

  @GetMapping("/{id}")
  @RequirePermission(PermissionCodes.B_PRO)
  public ProspeccaoResponse buscar(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
    return toResponse(service.buscar(principal, id));
  }

  @GetMapping("/{id}/eventos")
  @RequirePermission(PermissionCodes.B_PRO)
  public List<EventoResponse> eventos(
      @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
    return service.eventos(principal, id).stream()
        .map(e -> new EventoResponse(e.getId(), e.getTipo(), e.getPayload(), e.getAutorId(),
            e.getCreatedAt()))
        .toList();
  }

  // ─── Write ──────────────────────────────────────────────────────────────
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @RequirePermission(PermissionCodes.C_PRO)
  public ProspeccaoResponse criar(
      @AuthenticationPrincipal AuthPrincipal principal,
      @Valid @RequestBody ProspeccaoRequest req) {
    Prospeccao p = service.criar(
        principal,
        req.marcaId(),
        req.influenciadorId(),
        req.assessorResponsavelId(),
        req.titulo(),
        req.valorEstimadoCentavos(),
        req.proximaAcao(),
        req.proximaAcaoEm(),
        req.observacoes(),
        req.tags());
    return toResponse(p);
  }

  @PutMapping("/{id}")
  @RequirePermission(PermissionCodes.E_PRO)
  public ProspeccaoResponse atualizar(
      @AuthenticationPrincipal AuthPrincipal principal,
      @PathVariable UUID id,
      @Valid @RequestBody ProspeccaoRequest req) {
    return toResponse(
        service.atualizar(
            principal,
            id,
            req.marcaId(),
            req.influenciadorId(),
            req.assessorResponsavelId(),
            req.titulo(),
            req.valorEstimadoCentavos(),
            req.proximaAcao(),
            req.proximaAcaoEm(),
            req.observacoes(),
            req.tags()));
  }

  @PatchMapping("/{id}/status")
  @RequirePermission(PermissionCodes.E_PRO)
  public ProspeccaoResponse mudarStatus(
      @AuthenticationPrincipal AuthPrincipal principal,
      @PathVariable UUID id,
      @Valid @RequestBody StatusRequest req) {
    return toResponse(
        service.mudarStatus(
            principal, id, req.status(), req.motivoPerda(), req.motivoPerdaDetalhe()));
  }

  @PostMapping("/{id}/comentarios")
  @ResponseStatus(HttpStatus.CREATED)
  @RequirePermission(PermissionCodes.E_PRO)
  public EventoResponse comentar(
      @AuthenticationPrincipal AuthPrincipal principal,
      @PathVariable UUID id,
      @Valid @RequestBody ComentarioRequest req) {
    ProspeccaoEvento e = service.adicionarComentario(principal, id, req.texto());
    return new EventoResponse(e.getId(), e.getTipo(), e.getPayload(), e.getAutorId(),
        e.getCreatedAt());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RequirePermission(PermissionCodes.D_PRO)
  public void deletar(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
    service.deletar(principal, id);
  }

  private ProspeccaoResponse toResponse(Prospeccao p) {
    return new ProspeccaoResponse(
        p.getId(),
        p.getMarcaId(),
        p.getInfluenciadorId(),
        p.getAssessorResponsavelId(),
        p.getTitulo(),
        p.getStatus(),
        p.getValorEstimadoCentavos(),
        p.getProximaAcao(),
        p.getProximaAcaoEm(),
        p.getObservacoes(),
        List.of(p.getTags()),
        p.getMotivoPerda(),
        p.getMotivoPerdaDetalhe(),
        p.getFechadaEm(),
        p.getCreatedAt(),
        p.getUpdatedAt(),
        p.getCreatedBy());
  }
}
