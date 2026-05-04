package com.hubfeatcreators.domain.influenciador;

import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.web.PageResponse;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/v1/influenciadores")
public class InfluenciadorController {

    private final InfluenciadorService service;

    public InfluenciadorController(InfluenciadorService service) {
        this.service = service;
    }

    record InfluenciadorResponse(
            UUID id,
            String nome,
            Map<String, String> handles,
            String nicho,
            Long audienciaTotal,
            String observacoes,
            List<String> tags,
            Instant createdAt,
            Instant updatedAt) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InfluenciadorResponse criar(
            @Valid @RequestBody InfluenciadorRequest req,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return toResponse(service.criar(principal, req));
    }

    @GetMapping
    public PageResponse<InfluenciadorResponse> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String nicho,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Influenciador> result = service.listar(nome, nicho, page, size);
        List<InfluenciadorResponse> data =
                result.getContent().stream().map(this::toResponse).toList();
        String cursor = result.hasNext() ? String.valueOf(page + 1) : null;
        return PageResponse.of(data, cursor, size);
    }

    @GetMapping("/{id}")
    public InfluenciadorResponse buscar(@PathVariable UUID id) {
        return toResponse(service.buscar(id));
    }

    @PutMapping("/{id}")
    public InfluenciadorResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody InfluenciadorRequest req,
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
        List<Influenciador> all = service.exportar();
        StreamingResponseBody body =
                out -> {
                    out.write("id,nome,nicho,audiencia_total,tags,instagram,youtube\n".getBytes());
                    for (Influenciador inf : all) {
                        String line =
                                String.format(
                                        "%s,%s,%s,%s,%s,%s,%s\n",
                                        inf.getId(),
                                        escapeCsv(inf.getNome()),
                                        escapeCsv(inf.getNicho()),
                                        inf.getAudienciaTotal() != null
                                                ? inf.getAudienciaTotal()
                                                : "",
                                        escapeCsv(String.join("|", Arrays.asList(inf.getTags()))),
                                        escapeCsv(inf.getHandles().getOrDefault("instagram", "")),
                                        escapeCsv(inf.getHandles().getOrDefault("youtube", "")));
                        out.write(line.getBytes());
                    }
                };
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=influenciadores.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(body);
    }

    private InfluenciadorResponse toResponse(Influenciador inf) {
        return new InfluenciadorResponse(
                inf.getId(),
                inf.getNome(),
                inf.getHandles(),
                inf.getNicho(),
                inf.getAudienciaTotal(),
                inf.getObservacoes(),
                Arrays.asList(inf.getTags()),
                inf.getCreatedAt(),
                inf.getUpdatedAt());
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
