package com.hubfeatcreators.domain.portal;

import com.hubfeatcreators.config.AppProperties;
import com.hubfeatcreators.domain.influenciador.Influenciador;
import com.hubfeatcreators.domain.influenciador.InfluenciadorRepository;
import com.hubfeatcreators.infra.mail.SystemMailService;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.JwtService;
import com.hubfeatcreators.infra.web.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreatorAuthService {

    private static final int INVITE_DAYS = 7;

    private final CreatorUserRepository creatorUserRepo;
    private final CreatorInviteRepository inviteRepo;
    private final InfluenciadorRepository influenciadorRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SystemMailService mailService;
    private final AppProperties props;

    public CreatorAuthService(
            CreatorUserRepository creatorUserRepo,
            CreatorInviteRepository inviteRepo,
            InfluenciadorRepository influenciadorRepo,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            SystemMailService mailService,
            AppProperties props) {
        this.creatorUserRepo = creatorUserRepo;
        this.inviteRepo = inviteRepo;
        this.influenciadorRepo = influenciadorRepo;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.mailService = mailService;
        this.props = props;
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TokenResponse login(String email, String senha) {
        if (!props.getFeatures().isPortalEnabled()) {
            throw BusinessException.forbidden("PORTAL_DISABLED", "Portal não habilitado.");
        }
        CreatorUser user =
                creatorUserRepo
                        .findByEmail(email.toLowerCase())
                        .orElseThrow(
                                () -> BusinessException.unauthorized("Credenciais inválidas."));

        if (!"ATIVO".equals(user.getStatus())) {
            throw BusinessException.unauthorized("Conta inativa ou bloqueada.");
        }
        if (!passwordEncoder.matches(senha, user.getSenhaHash())) {
            throw BusinessException.unauthorized("Credenciais inválidas.");
        }

        String token =
                jwtService.generateCreatorToken(user.getId(), user.getInfluenciadorId());
        return new TokenResponse(token, user.getId(), user.getEmail(), user.getInfluenciadorId());
    }

    // ─── Convite ──────────────────────────────────────────────────────────────

    @Transactional
    public CreatorInvite convidar(AuthPrincipal principal, UUID influenciadorId, String email) {
        if (!props.getFeatures().isPortalEnabled()) {
            throw BusinessException.forbidden("PORTAL_DISABLED", "Portal não habilitado.");
        }

        if (!influenciadorRepo.existsById(influenciadorId)) {
            throw BusinessException.notFound();
        }

        if (email == null || email.isBlank()) {
            throw BusinessException.badRequest(
                    "INVITE_NO_EMAIL", "E-mail do destinatário é obrigatório.");
        }
        String targetEmail = email.toLowerCase();

        String rawToken = generateToken();
        String tokenHash = sha256(rawToken);
        Instant expiresAt = Instant.now().plus(INVITE_DAYS, ChronoUnit.DAYS);

        CreatorInvite invite =
                new CreatorInvite(
                        influenciadorId,
                        targetEmail,
                        tokenHash,
                        expiresAt,
                        principal.usuarioId());
        inviteRepo.save(invite);

        String portalUrl =
                props.getWeb().getBaseUrl() + "/portal/convite?token=" + rawToken;

        mailService.sendInvite(targetEmail, "HUB Feat Creators", portalUrl);

        return invite;
    }

    // ─── Aceitar convite ──────────────────────────────────────────────────────

    @Transactional
    public TokenResponse aceitarConvite(String rawToken, String senha) {
        String tokenHash = sha256(rawToken);
        CreatorInvite invite =
                inviteRepo
                        .findByTokenHash(tokenHash)
                        .orElseThrow(() -> BusinessException.notFound());

        if (!invite.isValido()) {
            throw BusinessException.gone("INVITE_EXPIRED", "Convite expirado ou já utilizado.");
        }

        if (creatorUserRepo.existsByInfluenciadorId(invite.getInfluenciadorId())) {
            throw BusinessException.conflict("CREATOR_ALREADY_EXISTS", "Creator já cadastrado.");
        }

        String senhaHash = passwordEncoder.encode(senha);
        CreatorUser user =
                new CreatorUser(
                        invite.getInfluenciadorId(),
                        invite.getEmail(),
                        senhaHash);
        creatorUserRepo.save(user);

        invite.setAceitoEm(Instant.now());
        inviteRepo.save(invite);

        String token =
                jwtService.generateCreatorToken(user.getId(), user.getInfluenciadorId());
        return new TokenResponse(token, user.getId(), user.getEmail(), user.getInfluenciadorId());
    }

    // ─── Token info ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public InviteInfo infoConvite(String rawToken) {
        String tokenHash = sha256(rawToken);
        CreatorInvite invite =
                inviteRepo.findByTokenHash(tokenHash).orElseThrow(BusinessException::notFound);
        if (!invite.isValido()) {
            throw BusinessException.gone("INVITE_EXPIRED", "Convite expirado ou já utilizado.");
        }
        String nomeInfluenciador =
                influenciadorRepo
                        .findById(invite.getInfluenciadorId())
                        .map(Influenciador::getNome)
                        .orElse("");
        return new InviteInfo(invite.getEmail(), nomeInfluenciador);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    public record TokenResponse(
            String token, UUID creatorUserId, String email, UUID influenciadorId) {}

    public record InviteInfo(String email, String nomeInfluenciador) {}
}
