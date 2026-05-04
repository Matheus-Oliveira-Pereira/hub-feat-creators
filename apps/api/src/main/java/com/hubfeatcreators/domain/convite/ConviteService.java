package com.hubfeatcreators.domain.convite;

import com.hubfeatcreators.config.AppProperties;
import com.hubfeatcreators.domain.assessoria.Assessoria;
import com.hubfeatcreators.domain.assessoria.AssessoriaRepository;
import com.hubfeatcreators.domain.onboarding.OnboardingService;
import com.hubfeatcreators.domain.rbac.Perfil;
import com.hubfeatcreators.domain.rbac.PerfilRepository;
import com.hubfeatcreators.domain.rbac.RbacBootstrap;
import com.hubfeatcreators.domain.usuario.Usuario;
import com.hubfeatcreators.domain.usuario.UsuarioRepository;
import com.hubfeatcreators.infra.audit.AuditLog;
import com.hubfeatcreators.infra.audit.AuditLogService;
import com.hubfeatcreators.infra.mail.SystemMailService;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.web.BusinessException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConviteService {

    private final ConviteRepository conviteRepo;
    private final UsuarioRepository usuarioRepo;
    private final AssessoriaRepository assessoriaRepo;
    private final PerfilRepository perfilRepo;
    private final PasswordEncoder passwordEncoder;
    private final RbacBootstrap rbacBootstrap;
    private final SystemMailService mailService;
    private final AuditLogService auditLogService;
    private final AppProperties props;

    public ConviteService(
            ConviteRepository conviteRepo,
            UsuarioRepository usuarioRepo,
            AssessoriaRepository assessoriaRepo,
            PerfilRepository perfilRepo,
            PasswordEncoder passwordEncoder,
            RbacBootstrap rbacBootstrap,
            SystemMailService mailService,
            AuditLogService auditLogService,
            AppProperties props) {
        this.conviteRepo = conviteRepo;
        this.usuarioRepo = usuarioRepo;
        this.assessoriaRepo = assessoriaRepo;
        this.perfilRepo = perfilRepo;
        this.passwordEncoder = passwordEncoder;
        this.rbacBootstrap = rbacBootstrap;
        this.mailService = mailService;
        this.auditLogService = auditLogService;
        this.props = props;
    }

    @Transactional
    public Convite convidar(AuthPrincipal principal, String email, Convite.Role role, UUID perfilId) {
        if (usuarioRepo.findActiveByAssessoriaIdAndEmail(principal.assessoriaId(), email).isPresent()) {
            throw BusinessException.conflict("EMAIL_IN_USE", "Este e-mail já é membro da assessoria.");
        }

        Perfil perfil = resolveProfile(principal.assessoriaId(), role, perfilId);

        String rawToken = UUID.randomUUID().toString();
        Convite convite = new Convite(
                principal.assessoriaId(), email, rawToken, role,
                Instant.now().plus(7, ChronoUnit.DAYS));

        convite.setPerfilId(perfil != null ? perfil.getId() : null);

        Usuario criador = usuarioRepo.findById(principal.usuarioId())
                .orElseThrow(() -> BusinessException.notFound("USUARIO"));
        convite.setCreatedBy(criador);

        conviteRepo.save(convite);

        Assessoria assessoria = assessoriaRepo.findById(principal.assessoriaId())
                .orElseThrow(() -> BusinessException.notFound("ASSESSORIA"));
        String inviteUrl = props.getWeb().getBaseUrl() + "/convite/" + rawToken;
        mailService.sendInvite(email, assessoria.getNome(), inviteUrl);

        auditLogService.log(principal.assessoriaId(), principal.usuarioId(),
                "convite", convite.getId(), AuditLog.Acao.INVITE_SENT, Map.of("email", email));

        return convite;
    }

    @Transactional
    public List<Convite> listar(AuthPrincipal principal) {
        return conviteRepo.findByAssessoriaIdAndUsedAtIsNull(principal.assessoriaId());
    }

    @Transactional
    public Usuario aceitarConvite(String rawToken, String senha) {
        Convite convite = conviteRepo.findByToken(rawToken)
                .orElseThrow(() -> BusinessException.badRequest("CONVITE_INVALIDO", "Convite inválido."));

        if (convite.isUsed()) {
            throw BusinessException.badRequest("CONVITE_USADO", "Este convite já foi utilizado.");
        }
        if (convite.isExpired()) {
            throw BusinessException.badRequest("CONVITE_EXPIRADO", "Este convite expirou.");
        }

        assessoriaRepo.findById(convite.getAssessoriaId())
                .orElseThrow(() -> BusinessException.notFound("ASSESSORIA"));

        Usuario.Role coarseRole = Usuario.Role.valueOf(convite.getRole().name());
        Usuario novo = new Usuario(
                convite.getAssessoriaId(), convite.getEmail(), passwordEncoder.encode(senha), coarseRole);
        novo.setEmailVerificadoEm(Instant.now()); // invite = email already verified

        UUID profileId = convite.getPerfilId();
        if (profileId == null) {
            Perfil seed = rbacBootstrap.seedFor(convite.getAssessoriaId(), coarseRole);
            profileId = seed.getId();
        }
        novo.setProfileId(profileId);

        Usuario usuario = usuarioRepo.save(novo);

        convite.setUsedAt(Instant.now());
        conviteRepo.save(convite);

        auditLogService.log(convite.getAssessoriaId(), usuario.getId(),
                "convite", convite.getId(), AuditLog.Acao.INVITE_ACCEPTED, Map.of("email", usuario.getEmail()));

        return usuario;
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private Perfil resolveProfile(UUID assessoriaId, Convite.Role role, UUID perfilId) {
        if (perfilId != null) {
            return perfilRepo.findById(perfilId).orElseThrow(() -> BusinessException.notFound("PERFIL"));
        }
        return rbacBootstrap.seedFor(assessoriaId, Usuario.Role.valueOf(role.name()));
    }
}
