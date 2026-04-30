package com.hubfeatcreators.infra.security;

import com.hubfeatcreators.domain.assessoria.Assessoria;
import com.hubfeatcreators.domain.assessoria.AssessoriaRepository;
import com.hubfeatcreators.domain.usuario.Usuario;
import com.hubfeatcreators.domain.usuario.UsuarioRepository;
import com.hubfeatcreators.infra.web.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private static final int REFRESH_TOKEN_DAYS = 30;

  private final AssessoriaRepository assessoriaRepo;
  private final UsuarioRepository usuarioRepo;
  private final RefreshTokenRepository refreshRepo;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;

  public AuthService(
      AssessoriaRepository assessoriaRepo,
      UsuarioRepository usuarioRepo,
      RefreshTokenRepository refreshRepo,
      JwtService jwtService,
      PasswordEncoder passwordEncoder) {
    this.assessoriaRepo = assessoriaRepo;
    this.usuarioRepo = usuarioRepo;
    this.refreshRepo = refreshRepo;
    this.jwtService = jwtService;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public TokenPair signup(
      String assessoriaNome, String slug, String email, String senha, HttpServletRequest req) {
    if (assessoriaRepo.existsBySlug(slug)) {
      throw BusinessException.conflict("SLUG_IN_USE", "Este slug já está em uso.");
    }
    if (usuarioRepo.findByEmail(email).isPresent()) {
      throw BusinessException.conflict("EMAIL_IN_USE", "Este e-mail já está cadastrado.");
    }

    Assessoria assessoria = assessoriaRepo.save(new Assessoria(assessoriaNome, slug));
    Usuario owner =
        usuarioRepo.save(
            new Usuario(
                assessoria.getId(),
                email,
                passwordEncoder.encode(senha),
                Usuario.Role.OWNER));

    return issueTokenPair(owner, assessoria.getId(), req);
  }

  @Transactional
  public TokenPair login(String email, String senha, HttpServletRequest req) {
    Usuario usuario =
        usuarioRepo
            .findByEmail(email)
            .filter(u -> u.getDeletedAt() == null)
            .orElseThrow(() -> BusinessException.unauthorized("Credenciais inválidas."));

    if (!passwordEncoder.matches(senha, usuario.getSenhaHash())) {
      throw BusinessException.unauthorized("Credenciais inválidas.");
    }

    usuario.setUltimoLoginEm(Instant.now());
    usuarioRepo.save(usuario);

    return issueTokenPair(usuario, usuario.getAssessoriaId(), req);
  }

  @Transactional
  public TokenPair refresh(String rawToken, HttpServletRequest req) {
    String hash = sha256(rawToken);
    RefreshToken stored =
        refreshRepo
            .findByTokenHash(hash)
            .orElseThrow(() -> BusinessException.unauthorized("Token inválido."));

    if (!stored.isValid()) {
      // Reuse detected — revoke entire family (token theft protection)
      refreshRepo.revokeFamily(stored.getFamilyId());
      throw BusinessException.unauthorized("Token revogado. Faça login novamente.");
    }

    stored.setRevokedAt(Instant.now());

    Usuario usuario =
        usuarioRepo
            .findById(stored.getUsuarioId())
            .orElseThrow(() -> BusinessException.unauthorized("Usuário não encontrado."));

    String newRaw = UUID.randomUUID().toString();
    RefreshToken newToken =
        new RefreshToken(
            sha256(newRaw),
            stored.getUsuarioId(),
            stored.getAssessoriaId(),
            stored.getFamilyId(),
            Instant.now().plus(REFRESH_TOKEN_DAYS, ChronoUnit.DAYS),
            req.getHeader("User-Agent"),
            req.getRemoteAddr());

    stored.setReplacedBy(newToken.getId());
    refreshRepo.save(stored);
    refreshRepo.save(newToken);

    String accessToken =
        jwtService.generateAccessToken(
            usuario.getId(), stored.getAssessoriaId(), usuario.getRole().name());

    return new TokenPair(accessToken, newRaw);
  }

  @Transactional
  public void logout(String rawToken) {
    refreshRepo
        .findByTokenHash(sha256(rawToken))
        .ifPresent(rt -> refreshRepo.revokeFamily(rt.getFamilyId()));
  }

  // ---- helpers ----

  private TokenPair issueTokenPair(Usuario usuario, UUID assessoriaId, HttpServletRequest req) {
    String rawRefresh = UUID.randomUUID().toString();
    UUID familyId = UUID.randomUUID();

    RefreshToken refreshToken =
        new RefreshToken(
            sha256(rawRefresh),
            usuario.getId(),
            assessoriaId,
            familyId,
            Instant.now().plus(REFRESH_TOKEN_DAYS, ChronoUnit.DAYS),
            req.getHeader("User-Agent"),
            req.getRemoteAddr());
    refreshRepo.save(refreshToken);

    String accessToken =
        jwtService.generateAccessToken(usuario.getId(), assessoriaId, usuario.getRole().name());

    return new TokenPair(accessToken, rawRefresh);
  }

  static String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public record TokenPair(String accessToken, String refreshToken) {}
}
