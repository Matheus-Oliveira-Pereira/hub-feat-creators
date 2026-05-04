package com.hubfeatcreators.infra.security;

import com.hubfeatcreators.config.AppProperties;
import com.hubfeatcreators.domain.assessoria.Assessoria;
import com.hubfeatcreators.domain.assessoria.AssessoriaRepository;
import com.hubfeatcreators.domain.onboarding.LoginLockoutService;
import com.hubfeatcreators.domain.onboarding.MfaService;
import com.hubfeatcreators.domain.onboarding.OnboardingService;
import com.hubfeatcreators.domain.rbac.Perfil;
import com.hubfeatcreators.domain.rbac.PerfilRepository;
import com.hubfeatcreators.domain.rbac.RbacBootstrap;
import com.hubfeatcreators.domain.usuario.Usuario;
import com.hubfeatcreators.domain.usuario.UsuarioRepository;
import com.hubfeatcreators.infra.audit.AuditLog;
import com.hubfeatcreators.infra.audit.AuditLogService;
import com.hubfeatcreators.infra.web.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
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
    private final RbacBootstrap rbacBootstrap;
    private final PerfilRepository perfilRepo;
    private final LoginLockoutService lockoutService;
    private final OnboardingService onboardingService;
    private final MfaService mfaService;
    private final AuditLogService auditLogService;
    private final AppProperties props;

    public AuthService(
            AssessoriaRepository assessoriaRepo,
            UsuarioRepository usuarioRepo,
            RefreshTokenRepository refreshRepo,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            RbacBootstrap rbacBootstrap,
            PerfilRepository perfilRepo,
            LoginLockoutService lockoutService,
            OnboardingService onboardingService,
            MfaService mfaService,
            AuditLogService auditLogService,
            AppProperties props) {
        this.assessoriaRepo = assessoriaRepo;
        this.usuarioRepo = usuarioRepo;
        this.refreshRepo = refreshRepo;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.rbacBootstrap = rbacBootstrap;
        this.perfilRepo = perfilRepo;
        this.lockoutService = lockoutService;
        this.onboardingService = onboardingService;
        this.mfaService = mfaService;
        this.auditLogService = auditLogService;
        this.props = props;
    }

    @Transactional
    public SignupResult signup(
            String assessoriaNome,
            String slug,
            String email,
            String senha,
            HttpServletRequest req) {
        if (!props.getFeatures().isSignupEnabled()) {
            throw BusinessException.forbidden("SIGNUP_DISABLED", "Novos cadastros estão temporariamente desativados.");
        }
        if (assessoriaRepo.existsBySlug(slug)) {
            throw BusinessException.conflict("SLUG_IN_USE", "Este slug já está em uso.");
        }
        if (usuarioRepo.findByEmail(email).isPresent()) {
            throw BusinessException.conflict("EMAIL_IN_USE", "Este e-mail já está cadastrado.");
        }

        Assessoria assessoria = assessoriaRepo.save(new Assessoria(assessoriaNome, slug));
        Perfil ownerProfile = rbacBootstrap.seedAssessoria(assessoria.getId());

        Usuario owner = new Usuario(assessoria.getId(), email, passwordEncoder.encode(senha), Usuario.Role.OWNER);
        owner.setProfileId(ownerProfile.getId());
        owner = usuarioRepo.save(owner);

        onboardingService.sendVerification(owner);

        auditLogService.logAuth(assessoria.getId(), owner.getId(), AuditLog.Acao.SIGNUP,
                Map.of("email", email, "slug", slug), ip(req), ua(req));

        return new SignupResult(owner.getId(), email, false);
    }

    @Transactional
    public TokenPair login(String email, String senha, String totpCode, HttpServletRequest req) {
        String lockKey = email.toLowerCase();
        lockoutService.checkLockout(lockKey);

        Usuario usuario = usuarioRepo.findByEmail(email)
                .filter(u -> u.getDeletedAt() == null)
                .orElse(null);

        if (usuario == null || !passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            if (usuario != null) {
                lockoutService.recordFailure(lockKey);
                auditLogService.logAuth(usuario.getAssessoriaId(), usuario.getId(),
                        AuditLog.Acao.LOGIN_FAILED, Map.of("reason", "BAD_CREDENTIALS"), ip(req), ua(req));
            }
            throw BusinessException.unauthorized("Credenciais inválidas.");
        }

        if (!usuario.isAtivo()) {
            throw BusinessException.unauthorized("Conta inativa. Entre em contato com o administrador.");
        }

        if (!usuario.isEmailVerificado()) {
            throw BusinessException.forbidden("EMAIL_NOT_VERIFIED", "Verifique seu e-mail antes de fazer login.");
        }

        // MFA check
        if (usuario.isMfaAtivo()) {
            if (totpCode == null || totpCode.isBlank()) {
                throw BusinessException.badRequest("MFA_REQUIRED", "Código MFA obrigatório.");
            }
            boolean valid = mfaService.verifyCode(usuario.getId(), totpCode);
            if (!valid) {
                valid = mfaService.useRecoveryCode(usuario.getId(), totpCode);
                if (valid) {
                    auditLogService.logAuth(usuario.getAssessoriaId(), usuario.getId(),
                            AuditLog.Acao.MFA_RECOVERY_USED, Map.of(), ip(req), ua(req));
                }
            }
            if (!valid) {
                lockoutService.recordFailure(lockKey);
                throw BusinessException.unauthorized("Código MFA inválido.");
            }
        }

        lockoutService.clearOnSuccess(lockKey);
        usuario.setUltimoLoginEm(Instant.now());
        usuarioRepo.save(usuario);

        auditLogService.logAuth(usuario.getAssessoriaId(), usuario.getId(),
                AuditLog.Acao.LOGIN, Map.of(), ip(req), ua(req));

        return issueTokenPair(usuario, usuario.getAssessoriaId(), req);
    }

    @Transactional
    public TokenPair refresh(String rawToken, HttpServletRequest req) {
        String hash = sha256(rawToken);
        RefreshToken stored = refreshRepo.findByTokenHash(hash)
                .orElseThrow(() -> BusinessException.unauthorized("Token inválido."));

        if (!stored.isValid()) {
            refreshRepo.revokeFamily(stored.getFamilyId());
            throw BusinessException.unauthorized("Token revogado. Faça login novamente.");
        }

        stored.setRevokedAt(Instant.now());

        Usuario usuario = usuarioRepo.findById(stored.getUsuarioId())
                .orElseThrow(() -> BusinessException.unauthorized("Usuário não encontrado."));

        String newRaw = UUID.randomUUID().toString();
        RefreshToken newToken = new RefreshToken(
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

        String accessToken = jwtService.generateAccessToken(
                usuario.getId(), stored.getAssessoriaId(), usuario.getRole().name(), permissionsOf(usuario));

        return new TokenPair(accessToken, newRaw);
    }

    @Transactional
    public void logout(String rawToken, HttpServletRequest req) {
        refreshRepo.findByTokenHash(sha256(rawToken)).ifPresent(rt -> {
            refreshRepo.revokeFamily(rt.getFamilyId());
            auditLogService.logAuth(rt.getAssessoriaId(), rt.getUsuarioId(),
                    AuditLog.Acao.LOGOUT, Map.of(), ip(req), ua(req));
        });
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private TokenPair issueTokenPair(Usuario usuario, UUID assessoriaId, HttpServletRequest req) {
        String rawRefresh = UUID.randomUUID().toString();
        UUID familyId = UUID.randomUUID();

        RefreshToken refreshToken = new RefreshToken(
                sha256(rawRefresh),
                usuario.getId(),
                assessoriaId,
                familyId,
                Instant.now().plus(REFRESH_TOKEN_DAYS, ChronoUnit.DAYS),
                req.getHeader("User-Agent"),
                req.getRemoteAddr());
        refreshRepo.save(refreshToken);

        String accessToken = jwtService.generateAccessToken(
                usuario.getId(), assessoriaId, usuario.getRole().name(), permissionsOf(usuario));

        return new TokenPair(accessToken, rawRefresh);
    }

    private Set<String> permissionsOf(Usuario usuario) {
        if (usuario.getProfileId() == null) return Set.of();
        return perfilRepo.findById(usuario.getProfileId()).map(Perfil::rolesAsSet).orElse(Set.of());
    }

    private static String ip(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        return xff != null ? xff.split(",")[0].trim() : req.getRemoteAddr();
    }

    private static String ua(HttpServletRequest req) {
        return req.getHeader("User-Agent");
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

    public Usuario getUsuario(UUID id) {
        return usuarioRepo.findById(id)
                .orElseThrow(() -> BusinessException.notFound("USUARIO"));
    }

    public record TokenPair(String accessToken, String refreshToken) {}

    public record SignupResult(UUID usuarioId, String email, boolean emailVerificado) {}
}
