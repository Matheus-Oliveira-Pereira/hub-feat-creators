package com.hubfeatcreators.domain.usuario;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import com.hubfeatcreators.infra.web.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

  private final UsuarioRepository usuarioRepo;

  public UsuarioController(UsuarioRepository usuarioRepo) {
    this.usuarioRepo = usuarioRepo;
  }

  public record AtribuirPerfilRequest(@NotNull UUID profileId) {}

  @PatchMapping("/{id}/profile")
  @RequirePermission(PermissionCodes.E_USU)
  @Transactional
  public void atribuirPerfil(
      @AuthenticationPrincipal AuthPrincipal principal,
      @PathVariable UUID id,
      @Valid @RequestBody AtribuirPerfilRequest req) {
    Usuario u =
        usuarioRepo.findById(id).orElseThrow(() -> BusinessException.notFound("USUARIO"));
    if (!u.getAssessoriaId().equals(principal.assessoriaId())) {
      throw BusinessException.notFound("USUARIO");
    }
    u.setProfileId(req.profileId());
    usuarioRepo.save(u);
  }
}
