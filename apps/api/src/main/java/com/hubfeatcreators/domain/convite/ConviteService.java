package com.hubfeatcreators.domain.convite;

import com.hubfeatcreators.domain.assessoria.AssessoriaRepository;
import com.hubfeatcreators.domain.usuario.Usuario;
import com.hubfeatcreators.domain.usuario.UsuarioRepository;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.web.BusinessException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConviteService {

  private final ConviteRepository conviteRepo;
  private final UsuarioRepository usuarioRepo;
  private final AssessoriaRepository assessoriaRepo;
  private final PasswordEncoder passwordEncoder;

  public ConviteService(
      ConviteRepository conviteRepo,
      UsuarioRepository usuarioRepo,
      AssessoriaRepository assessoriaRepo,
      PasswordEncoder passwordEncoder) {
    this.conviteRepo = conviteRepo;
    this.usuarioRepo = usuarioRepo;
    this.assessoriaRepo = assessoriaRepo;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public Convite convidar(AuthPrincipal principal, String email, Convite.Role role) {
    if (usuarioRepo.findActiveByAssessoriaIdAndEmail(principal.assessoriaId(), email).isPresent()) {
      throw BusinessException.conflict("EMAIL_IN_USE", "Este e-mail já é membro da assessoria.");
    }

    Convite convite =
        new Convite(
            principal.assessoriaId(),
            email,
            UUID.randomUUID().toString(),
            role,
            Instant.now().plus(72, ChronoUnit.HOURS));

    Usuario criador =
        usuarioRepo
            .findById(principal.usuarioId())
            .orElseThrow(() -> BusinessException.notFound("USUARIO"));
    convite.setCreatedBy(criador);

    return conviteRepo.save(convite);
  }

  @Transactional
  public Usuario aceitarConvite(String token, String senha) {
    Convite convite =
        conviteRepo
            .findByToken(token)
            .orElseThrow(() -> BusinessException.badRequest("CONVITE_INVALIDO", "Convite inválido."));

    if (convite.isUsed()) {
      throw BusinessException.badRequest("CONVITE_USADO", "Este convite já foi utilizado.");
    }
    if (convite.isExpired()) {
      throw BusinessException.badRequest("CONVITE_EXPIRADO", "Este convite expirou.");
    }

    assessoriaRepo
        .findById(convite.getAssessoriaId())
        .orElseThrow(() -> BusinessException.notFound("ASSESSORIA"));

    Usuario usuario =
        usuarioRepo.save(
            new Usuario(
                convite.getAssessoriaId(),
                convite.getEmail(),
                passwordEncoder.encode(senha),
                Usuario.Role.valueOf(convite.getRole().name())));

    convite.setUsedAt(Instant.now());
    conviteRepo.save(convite);

    return usuario;
  }
}
