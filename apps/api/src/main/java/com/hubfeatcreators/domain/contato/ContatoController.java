package com.hubfeatcreators.domain.contato;

import com.hubfeatcreators.infra.security.AuthPrincipal;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contatos")
public class ContatoController {

    private final ContatoService service;

    public ContatoController(ContatoService service) {
        this.service = service;
    }

    record ContatoResponse(
            UUID id,
            UUID marcaId,
            String nome,
            String email,
            String telefone,
            String cargo,
            boolean emailInvalido,
            Instant createdAt,
            Instant updatedAt) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContatoResponse criar(
            @Valid @RequestBody ContatoRequest req,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return toResponse(service.criar(principal, req));
    }

    @GetMapping
    public List<ContatoResponse> listarPorMarca(@RequestParam UUID marcaId) {
        return service.listarPorMarca(marcaId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ContatoResponse buscar(@PathVariable UUID id) {
        return toResponse(service.buscar(id));
    }

    @PutMapping("/{id}")
    public ContatoResponse atualizar(
            @PathVariable UUID id,
            @Valid @RequestBody ContatoRequest req,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return toResponse(service.atualizar(principal, id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletar(@PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
        service.deletar(principal, id);
    }

    private ContatoResponse toResponse(Contato c) {
        return new ContatoResponse(
                c.getId(),
                c.getMarcaId(),
                c.getNome(),
                c.getEmail(),
                c.getTelefone(),
                c.getCargo(),
                c.isEmailInvalido(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
