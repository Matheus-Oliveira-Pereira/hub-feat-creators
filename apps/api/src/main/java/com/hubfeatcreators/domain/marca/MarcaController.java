package com.hubfeatcreators.domain.marca;

import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.web.PageResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1/marcas")
public class MarcaController {

  private final MarcaService service;

  public MarcaController(MarcaService service) {
    this.service = service;
  }

  record MarcaResponse(
      UUID id, String nome, String segmento, String site, String observacoes,
      List<String> tags, Instant createdAt, Instant updatedAt) {}

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public MarcaResponse criar(
      @Valid @RequestBody MarcaRequest req, @AuthenticationPrincipal AuthPrincipal principal) {
    return toResponse(service.criar(principal, req));
  }

  @GetMapping
  public PageResponse<MarcaResponse> listar(
      @RequestParam(required = false) String nome,
      @RequestParam(required = false) String segmento,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Page<Marca> result = service.listar(nome, segmento, page, size);
    List<MarcaResponse> data = result.getContent().stream().map(this::toResponse).toList();
    return PageResponse.of(data, result.hasNext() ? String.valueOf(page + 1) : null, size);
  }

  @GetMapping("/{id}")
  public MarcaResponse buscar(@PathVariable UUID id) {
    return toResponse(service.buscar(id));
  }

  @PutMapping("/{id}")
  public MarcaResponse atualizar(
      @PathVariable UUID id,
      @Valid @RequestBody MarcaRequest req,
      @AuthenticationPrincipal AuthPrincipal principal) {
    return toResponse(service.atualizar(principal, id, req));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletar(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
    service.deletar(principal, id);
  }

  @GetMapping("/export.csv")
  public ResponseEntity<StreamingResponseBody> exportCsv() {
    List<Marca> all = service.exportar();
    StreamingResponseBody body =
        out -> {
          out.write("id,nome,segmento,site,tags\n".getBytes());
          for (Marca m : all) {
            String line = String.format("%s,%s,%s,%s,%s\n",
                m.getId(), escapeCsv(m.getNome()), escapeCsv(m.getSegmento()),
                escapeCsv(m.getSite()), escapeCsv(String.join("|", Arrays.asList(m.getTags()))));
            out.write(line.getBytes());
          }
        };
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=marcas.csv")
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .body(body);
  }

  private MarcaResponse toResponse(Marca m) {
    return new MarcaResponse(m.getId(), m.getNome(), m.getSegmento(), m.getSite(),
        m.getObservacoes(), Arrays.asList(m.getTags()), m.getCreatedAt(), m.getUpdatedAt());
  }

  private String escapeCsv(String val) {
    if (val == null) return "";
    if (val.contains(",") || val.contains("\"") || val.contains("\n"))
      return "\"" + val.replace("\"", "\"\"") + "\"";
    return val;
  }
}
