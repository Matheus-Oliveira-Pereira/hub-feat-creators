package com.hubfeatcreators.domain.prospeccao;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import com.hubfeatcreators.infra.web.PageResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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

  @GetMapping(value = "/export.csv", produces = "text/csv; charset=UTF-8")
  @RequirePermission(PermissionCodes.EXPT)
  public void exportCsv(
      @AuthenticationPrincipal AuthPrincipal principal,
      @RequestParam(required = false) ProspeccaoStatus status,
      @RequestParam(required = false) UUID assessorId,
      @RequestParam(required = false) UUID marcaId,
      @RequestParam(required = false) String nome,
      HttpServletResponse response) throws IOException {
    response.setContentType("text/csv; charset=UTF-8");
    response.setHeader(
        "Content-Disposition",
        "attachment; filename=\"prospeccoes-" + Instant.now().getEpochSecond() + ".csv\"");

    int pageSize = 500;
    int max = 10_000;
    int written = 0;

    try (OutputStream os = response.getOutputStream();
        Writer w = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
      // BOM pra Excel reconhecer UTF-8
      w.write('﻿');
      w.write("id,titulo,status,marca_id,influenciador_id,assessor_id,"
          + "valor_estimado_centavos,proxima_acao,proxima_acao_em,tags,"
          + "motivo_perda,fechada_em,created_at\n");

      int page = 0;
      while (written < max) {
        Page<Prospeccao> chunk =
            service.listar(principal, status, assessorId, marcaId, nome, page, pageSize);
        if (chunk.isEmpty()) break;
        for (Prospeccao p : chunk.getContent()) {
          if (written >= max) break;
          w.write(toCsvRow(p));
          w.write('\n');
          written++;
        }
        if (!chunk.hasNext()) break;
        page++;
        w.flush();
      }
    }
  }

  private String toCsvRow(Prospeccao p) {
    return String.join(
        ",",
        csv(p.getId().toString()),
        csv(p.getTitulo()),
        csv(p.getStatus().name()),
        csv(p.getMarcaId().toString()),
        csv(p.getInfluenciadorId() != null ? p.getInfluenciadorId().toString() : ""),
        csv(p.getAssessorResponsavelId().toString()),
        csv(p.getValorEstimadoCentavos() != null ? p.getValorEstimadoCentavos().toString() : ""),
        csv(p.getProximaAcao() != null ? p.getProximaAcao() : ""),
        csv(p.getProximaAcaoEm() != null ? p.getProximaAcaoEm().toString() : ""),
        csv(String.join(";", p.getTags())),
        csv(p.getMotivoPerda() != null ? p.getMotivoPerda().name() : ""),
        csv(p.getFechadaEm() != null ? p.getFechadaEm().toString() : ""),
        csv(p.getCreatedAt().toString()));
  }

  private String csv(String s) {
    if (s == null) return "";
    if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
      return "\"" + s.replace("\"", "\"\"") + "\"";
    }
    return s;
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
