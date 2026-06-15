package com.hubfeatcreators.infra.bootstrap;

import com.hubfeatcreators.config.AppProperties;
import com.hubfeatcreators.domain.rbac.Perfil;
import com.hubfeatcreators.domain.rbac.RbacBootstrap;
import com.hubfeatcreators.domain.usuario.Usuario;
import com.hubfeatcreators.domain.usuario.UsuarioRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Garante que ao menos um usuário OWNER exista após o boot. Roda em ApplicationReadyEvent — Flyway
 * já aplicou migrations, JPA está pronto. Controlado por FEATURE_ADMIN_BOOTSTRAP_ENABLED (default
 * true).
 */
@Component
public class AdminBootstrap {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UsuarioRepository usuarioRepo;
    private final RbacBootstrap rbacBootstrap;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;

    public AdminBootstrap(
            UsuarioRepository usuarioRepo,
            RbacBootstrap rbacBootstrap,
            PasswordEncoder passwordEncoder,
            AppProperties props) {
        this.usuarioRepo = usuarioRepo;
        this.rbacBootstrap = rbacBootstrap;
        this.passwordEncoder = passwordEncoder;
        this.props = props;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensureAdminExists() {
        if (!props.getFeatures().isAdminBootstrapEnabled()) {
            log.debug("admin.bootstrap.disabled");
            return;
        }

        if (usuarioRepo.existsByRoleAndDeletedAtIsNull(Usuario.Role.OWNER)) {
            log.debug("admin.bootstrap.skip motivo=owner_exists");
            return;
        }

        String email = props.getAdmin().getEmail();
        String rawPassword = props.getAdmin().getPassword();

        Perfil ownerPerfil = rbacBootstrap.ensureSystemPerfis();

        Usuario admin = new Usuario(email, passwordEncoder.encode(rawPassword), Usuario.Role.OWNER);
        admin.setProfileId(ownerPerfil.getId());
        admin.setEmailVerificadoEm(Instant.now());
        usuarioRepo.save(admin);

        log.warn(
                "admin.bootstrap.created email={} — troque a senha imediatamente em producao",
                email);
    }
}
