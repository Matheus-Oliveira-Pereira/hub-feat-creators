package com.hubfeatcreators.domain.onboarding;

import com.hubfeatcreators.config.AppProperties;
import com.hubfeatcreators.domain.usuario.Usuario;
import com.hubfeatcreators.domain.usuario.UsuarioRepository;
import com.hubfeatcreators.infra.mail.SystemMailService;
import com.hubfeatcreators.infra.web.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OnboardingService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);

    private final EmailVerifyTokenRepository verifyRepo;
    private final PasswordResetTokenRepository resetRepo;
    private final UsuarioRepository usuarioRepo;
    private final PasswordEncoder passwordEncoder;
    private final SystemMailService mailService;
    private final AppProperties props;

    public OnboardingService(
            EmailVerifyTokenRepository verifyRepo,
            PasswordResetTokenRepository resetRepo,
            UsuarioRepository usuarioRepo,
            PasswordEncoder passwordEncoder,
            SystemMailService mailService,
            AppProperties props) {
        this.verifyRepo = verifyRepo;
        this.resetRepo = resetRepo;
        this.usuarioRepo = usuarioRepo;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.props = props;
    }

    // ── Email verification ────────────────────────────────────────────────

    @Transactional
    public void sendVerification(Usuario usuario) {
        verifyRepo.invalidateExisting(usuario.getId());

        String raw = UUID.randomUUID().toString();
        EmailVerifyToken token = new EmailVerifyToken(
                usuario.getId(), sha256(raw), Instant.now().plus(24, ChronoUnit.HOURS));
        verifyRepo.save(token);

        String url = props.getWeb().getBaseUrl() + "/verify-email?token=" + raw;
        mailService.sendVerifyEmail(usuario.getEmail(), url);
        log.info("onboarding.verify.sent usuarioId={}", usuario.getId());
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        String hash = sha256(rawToken);
        EmailVerifyToken token = verifyRepo.findByTokenHash(hash)
                .orElseThrow(() -> BusinessException.badRequest("TOKEN_INVALIDO", "Token inválido."));

        if (!token.isValid()) {
            throw BusinessException.gone("TOKEN_EXPIRADO", "Token expirado ou já utilizado.");
        }

        token.setUsedAt(Instant.now());
        verifyRepo.save(token);

        Usuario usuario = usuarioRepo.findById(token.getUsuarioId())
                .orElseThrow(() -> BusinessException.notFound("USUARIO"));
        usuario.setEmailVerificadoEm(Instant.now());
        usuario.setUpdatedAt(Instant.now());
        usuarioRepo.save(usuario);

        log.info("onboarding.email.verified usuarioId={}", usuario.getId());
    }

    // ── Password reset ────────────────────────────────────────────────────

    @Transactional
    public void requestPasswordReset(String email) {
        // Always return OK (don't leak existence)
        usuarioRepo.findByEmail(email)
                .filter(u -> u.getDeletedAt() == null)
                .ifPresent(u -> {
                    resetRepo.invalidateExisting(u.getId());
                    String raw = UUID.randomUUID().toString();
                    PasswordResetToken token = new PasswordResetToken(
                            u.getId(), sha256(raw), Instant.now().plus(1, ChronoUnit.HOURS));
                    resetRepo.save(token);
                    String url = props.getWeb().getBaseUrl() + "/reset-password?token=" + raw;
                    mailService.sendPasswordReset(u.getEmail(), url);
                    log.info("onboarding.reset.sent usuarioId={}", u.getId());
                });
    }

    @Transactional
    public void resetPassword(String rawToken, String novaSenha) {
        String hash = sha256(rawToken);
        PasswordResetToken token = resetRepo.findByTokenHash(hash)
                .orElseThrow(() -> BusinessException.badRequest("TOKEN_INVALIDO", "Token inválido."));

        if (!token.isValid()) {
            throw BusinessException.gone("TOKEN_EXPIRADO", "Token expirado ou já utilizado.");
        }

        token.setUsedAt(Instant.now());
        resetRepo.save(token);

        Usuario usuario = usuarioRepo.findById(token.getUsuarioId())
                .orElseThrow(() -> BusinessException.notFound("USUARIO"));
        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuario.setUpdatedAt(Instant.now());
        usuarioRepo.save(usuario);

        log.info("onboarding.password.reset usuarioId={}", usuario.getId());
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
