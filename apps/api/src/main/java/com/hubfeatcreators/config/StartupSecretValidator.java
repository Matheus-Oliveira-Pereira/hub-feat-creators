package com.hubfeatcreators.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Valida no boot que segredos críticos não estão em valores default. Em profile != "dev", boot
 * falha (logging + System.exit) se algum segredo for default ou curto demais.
 */
@Component
public class StartupSecretValidator {

    private static final Logger log = LoggerFactory.getLogger(StartupSecretValidator.class);

    private static final List<String> DEV_PREFIXES =
            List.of("dev-only-not-for-prod", "test-secret-must-be");

    private static final int MIN_KEY_BYTES = 32;

    private final AppProperties props;
    private final String activeProfile;

    public StartupSecretValidator(
            AppProperties props, @Value("${spring.profiles.active:default}") String activeProfile) {
        this.props = props;
        this.activeProfile = activeProfile;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        boolean isProd = !activeProfile.contains("dev") && !activeProfile.contains("test");
        if (!isProd) {
            log.info("startup.secret.validator profile={} → skip", activeProfile);
            return;
        }

        check("app.jwt.secret", props.getJwt().getSecret(), 64, isProd);
        check("app.secrets.email-key", props.getSecrets().getEmailKey(), MIN_KEY_BYTES, isProd);
        check(
                "app.secrets.whatsapp-key",
                props.getSecrets().getWhatsappKey(),
                MIN_KEY_BYTES,
                isProd);
        check("app.secrets.social-key", props.getSecrets().getSocialKey(), MIN_KEY_BYTES, isProd);
        check("app.secrets.mfa-key", props.getSecrets().getMfaKey(), MIN_KEY_BYTES, isProd);
        check("app.whatsapp.verify-token", props.getWhatsapp().getVerifyToken(), 16, isProd);

        log.info("startup.secret.validator profile={} ok", activeProfile);
    }

    private void check(String name, String value, int minBytes, boolean failHard) {
        if (value == null || value.isBlank()) {
            fail(name + " ausente em profile prod");
        }
        for (String prefix : DEV_PREFIXES) {
            if (value.startsWith(prefix)) {
                fail(name + " usa valor default — defina via env var");
            }
        }
        if (value.getBytes().length < minBytes) {
            fail(name + " curto demais (precisa ≥ " + minBytes + " bytes)");
        }
    }

    private void fail(String msg) {
        log.error("startup.secret.validator FAIL: {}", msg);
        throw new IllegalStateException("Segredo inválido em prod: " + msg);
    }
}
