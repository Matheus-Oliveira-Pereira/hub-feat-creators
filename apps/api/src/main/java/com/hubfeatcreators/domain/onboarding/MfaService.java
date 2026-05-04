package com.hubfeatcreators.domain.onboarding;

import com.hubfeatcreators.domain.usuario.Usuario;
import com.hubfeatcreators.domain.usuario.UsuarioRepository;
import com.hubfeatcreators.infra.web.BusinessException;
import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import dev.samstevens.totp.util.Utils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaService.class);
    private static final int RECOVERY_CODE_COUNT = 10;

    private final UsuarioRepository usuarioRepo;
    private final MfaRecoveryCodeRepository recoveryRepo;
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeVerifier codeVerifier;

    public MfaService(UsuarioRepository usuarioRepo, MfaRecoveryCodeRepository recoveryRepo) {
        this.usuarioRepo = usuarioRepo;
        this.recoveryRepo = recoveryRepo;
        TimeProvider timeProvider = new SystemTimeProvider();
        CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
        this.codeVerifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    }

    public record MfaSetupData(String secret, String qrCodeUri, List<String> recoveryCodes) {}

    /** Generates TOTP secret + QR + recovery codes. Does NOT activate MFA yet. */
    @Transactional
    public MfaSetupData setupMfa(UUID usuarioId) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> BusinessException.notFound("USUARIO"));

        String secret = secretGenerator.generate();
        usuario.setMfaSecretEnc(secret); // TODO: encrypt with AES-GCM in prod
        usuario.setUpdatedAt(Instant.now());
        usuarioRepo.save(usuario);

        String qrUri = generateQrUri(usuario.getEmail(), secret);
        List<String> rawCodes = generateRecoveryCodes(usuarioId);

        log.info("mfa.setup.initiated usuarioId={}", usuarioId);
        return new MfaSetupData(secret, qrUri, rawCodes);
    }

    /** Verifies TOTP code and activates MFA. */
    @Transactional
    public void activateMfa(UUID usuarioId, String totpCode) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> BusinessException.notFound("USUARIO"));

        if (usuario.getMfaSecretEnc() == null) {
            throw BusinessException.badRequest("MFA_NOT_SETUP", "Execute /mfa/setup primeiro.");
        }

        if (!codeVerifier.isValidCode(usuario.getMfaSecretEnc(), totpCode)) {
            throw BusinessException.badRequest("MFA_CODE_INVALID", "Código TOTP inválido.");
        }

        usuario.setMfaAtivo(true);
        usuario.setUpdatedAt(Instant.now());
        usuarioRepo.save(usuario);

        log.info("mfa.activated usuarioId={}", usuarioId);
    }

    /** Verifies a TOTP code (at login). Returns true if valid. */
    public boolean verifyCode(UUID usuarioId, String totpCode) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> BusinessException.notFound("USUARIO"));

        if (!usuario.isMfaAtivo() || usuario.getMfaSecretEnc() == null) return false;
        return codeVerifier.isValidCode(usuario.getMfaSecretEnc(), totpCode);
    }

    /** Uses a recovery code. Single-use. Returns true if valid + marks used. */
    @Transactional
    public boolean useRecoveryCode(UUID usuarioId, String rawCode) {
        String hash = sha256(rawCode.trim().toUpperCase());
        return recoveryRepo.findByCodeHashAndUsedAtIsNull(hash)
                .filter(c -> c.getUsuarioId().equals(usuarioId))
                .map(c -> {
                    c.setUsedAt(Instant.now());
                    recoveryRepo.save(c);
                    log.info("mfa.recovery.used usuarioId={}", usuarioId);
                    return true;
                })
                .orElse(false);
    }

    /** Disables MFA and clears secret + recovery codes. */
    @Transactional
    public void disableMfa(UUID usuarioId, String totpCode) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> BusinessException.notFound("USUARIO"));

        if (!usuario.isMfaAtivo()) return;

        if (!codeVerifier.isValidCode(usuario.getMfaSecretEnc(), totpCode)) {
            throw BusinessException.badRequest("MFA_CODE_INVALID", "Código TOTP inválido.");
        }

        usuario.setMfaAtivo(false);
        usuario.setMfaSecretEnc(null);
        usuario.setUpdatedAt(Instant.now());
        usuarioRepo.save(usuario);
        recoveryRepo.deleteAllByUsuarioId(usuarioId);

        log.info("mfa.disabled usuarioId={}", usuarioId);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private String generateQrUri(String email, String secret) {
        try {
            QrData data = new QrData.Builder()
                    .label(email)
                    .secret(secret)
                    .issuer("feat. creators")
                    .algorithm(HashingAlgorithm.SHA1)
                    .digits(6)
                    .period(30)
                    .build();
            QrGenerator generator = new ZxingPngQrGenerator();
            byte[] imageData = generator.generate(data);
            return Utils.getDataUriForImage(imageData, generator.getImageMimeType());
        } catch (Exception e) {
            log.error("mfa.qr.error email={} error={}", email, e.getMessage());
            throw new IllegalStateException("QR code generation failed", e);
        }
    }

    private List<String> generateRecoveryCodes(UUID usuarioId) {
        recoveryRepo.deleteAllByUsuarioId(usuarioId);
        SecureRandom rng = new SecureRandom();
        List<String> raw = new ArrayList<>();
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            String code = String.format("%05d-%05d", rng.nextInt(100000), rng.nextInt(100000));
            raw.add(code);
            recoveryRepo.save(new MfaRecoveryCode(usuarioId, sha256(code.toUpperCase())));
        }
        return raw;
    }

    static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
