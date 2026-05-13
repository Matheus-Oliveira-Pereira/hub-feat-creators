package com.hubfeatcreators.domain.rbac;

import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/perfis")
public class PerfilController {

    private final PerfilService service;
    private final PerfilRepository repo;

    public PerfilController(PerfilService service, PerfilRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    public record PerfilResponse(
            UUID id,
            String nome,
            String descricao,
            List<String> roles,
            boolean isSystem,
            long usuariosCount,
            Instant createdAt,
            Instant updatedAt) {}

    public record PerfilRequest(
            @NotBlank @Size(max = 80) String nome,
            @Size(max = 240) String descricao,
            @NotNull Set<String> roles) {}

    @GetMapping
    @RequirePermission(PermissionCodes.B_PRF)
    public List<PerfilResponse> listar(@AuthenticationPrincipal AuthPrincipal principal) {
        List<Perfil> perfis = service.listar(principal);
        if (perfis.isEmpty()) return List.of();
        List<UUID> ids = perfis.stream().map(Perfil::getId).toList();
        Map<UUID, Long> counts =
                repo.countsByPerfilIds(ids).stream()
                        .collect(Collectors.toMap(r -> (UUID) r[0], r -> (Long) r[1]));
        return perfis.stream().map(p -> toResponse(p, counts.getOrDefault(p.getId(), 0L))).toList();
    }

    @GetMapping("/{id}")
    @RequirePermission(PermissionCodes.B_PRF)
    public PerfilResponse buscar(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        Perfil p = service.buscar(principal, id);
        return toResponse(p, repo.countUsuariosUsando(p.getId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.C_PRF)
    public PerfilResponse criar(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody PerfilRequest req) {
        return toResponse(service.criar(principal, req.nome(), req.descricao(), req.roles()), 0L);
    }

    @PutMapping("/{id}")
    @RequirePermission(PermissionCodes.E_PRF)
    public PerfilResponse atualizar(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody PerfilRequest req) {
        Perfil p = service.atualizar(principal, id, req.nome(), req.descricao(), req.roles());
        return toResponse(p, repo.countUsuariosUsando(p.getId()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.D_PRF)
    public void deletar(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        service.deletar(principal, id);
    }

    private PerfilResponse toResponse(Perfil p, long usuariosCount) {
        return new PerfilResponse(
                p.getId(),
                p.getNome(),
                p.getDescricao(),
                List.of(p.getRoles()),
                p.isSystem(),
                usuariosCount,
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
