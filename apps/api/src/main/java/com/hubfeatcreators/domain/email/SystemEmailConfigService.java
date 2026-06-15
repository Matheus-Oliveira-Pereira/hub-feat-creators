package com.hubfeatcreators.domain.email;

import com.hubfeatcreators.config.AppProperties;
import com.hubfeatcreators.domain.notificacao.events.EmailAuthFalhouEvent;
import com.hubfeatcreators.infra.web.BusinessException;
import jakarta.mail.Session;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemEmailConfigService {

    private static final Logger log = LoggerFactory.getLogger(SystemEmailConfigService.class);
    private static final int CIRCUIT_BREAKER_THRESHOLD = 3;
    private static final Duration CIRCUIT_BREAKER_WINDOW = Duration.ofMinutes(10);

    private final SystemEmailConfigRepository repo;
    private final EmailCipherService cipher;
    private final AppProperties props;
    private final ApplicationEventPublisher eventPublisher;

    // Cache: invalidado ao salvar ou testar
    private volatile JavaMailSender cachedSender;

    public SystemEmailConfigService(
            SystemEmailConfigRepository repo,
            EmailCipherService cipher,
            AppProperties props,
            ApplicationEventPublisher eventPublisher) {
        this.repo = repo;
        this.cipher = cipher;
        this.props = props;
        this.eventPublisher = eventPublisher;
    }

    // ── Config read ────────────────────────────────────────────────────────

    /** Returns DB config if present, else builds transient config from env vars. */
    public Optional<SystemEmailConfig> getEffectiveConfig() {
        Optional<SystemEmailConfig> db = repo.findFirst();
        if (db.isPresent()) return db;

        AppProperties.Smtp smtp = props.getSmtp();
        if (smtp.getHost().isBlank()) return Optional.empty();

        // Synthetic in-memory config from env vars (not persisted)
        SystemEmailConfig fallback = new SystemEmailConfig();
        fallback.setHost(smtp.getHost());
        fallback.setPort(smtp.getPort());
        fallback.setUsername(smtp.getUsername());
        fallback.setFromAddress(smtp.getFromAddress());
        fallback.setFromName(smtp.getFromName());
        fallback.setTlsMode(smtp.getTlsMode());
        fallback.setDailyQuota(smtp.getDailyQuota());
        // Password stored plaintext in env var (dev/test); no cipher
        return Optional.of(fallback);
    }

    /** Returns decrypted password for effective config. */
    public String getEffectivePassword() {
        Optional<SystemEmailConfig> db = repo.findFirst();
        if (db.isPresent()) {
            SystemEmailConfig cfg = db.get();
            return cipher.decrypt(cfg.getPasswordEnc(), cfg.getPasswordNonce());
        }
        return props.getSmtp().getPassword();
    }

    /**
     * Builds (and caches) a JavaMailSender from effective config. Returns empty if not configured.
     */
    public Optional<JavaMailSender> getSender() {
        if (cachedSender != null) return Optional.of(cachedSender);

        Optional<SystemEmailConfig> cfgOpt = getEffectiveConfig();
        if (cfgOpt.isEmpty()) return Optional.empty();

        SystemEmailConfig cfg = cfgOpt.get();
        String password = getEffectivePassword();

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(cfg.getHost());
        sender.setPort(cfg.getPort());
        sender.setUsername(cfg.getUsername());
        sender.setPassword(password);

        Properties javaMailProps = new Properties();
        javaMailProps.put("mail.smtp.auth", "true");
        javaMailProps.put("mail.smtp.timeout", "30000");
        javaMailProps.put("mail.smtp.connectiontimeout", "15000");
        if ("SSL".equalsIgnoreCase(cfg.getTlsMode())) {
            javaMailProps.put("mail.smtp.ssl.enable", "true");
        } else {
            javaMailProps.put("mail.smtp.starttls.enable", "true");
            javaMailProps.put("mail.smtp.starttls.required", "true");
        }
        sender.setJavaMailProperties(javaMailProps);

        cachedSender = sender;
        return Optional.of(sender);
    }

    // ── Config write ───────────────────────────────────────────────────────

    @Transactional
    public SystemEmailConfig salvar(
            String host,
            Integer port,
            String username,
            String password,
            String fromAddress,
            String fromName,
            String tlsMode,
            Integer dailyQuota) {
        SystemEmailConfig cfg = repo.findFirst().orElse(new SystemEmailConfig());
        if (host != null) cfg.setHost(host);
        if (port != null) cfg.setPort(port);
        if (username != null) cfg.setUsername(username);
        if (password != null) {
            EmailCipherService.Encrypted enc = cipher.encrypt(password);
            cfg.setPasswordEnc(enc.ciphertext());
            cfg.setPasswordNonce(enc.nonce());
        }
        if (fromAddress != null) cfg.setFromAddress(fromAddress);
        if (fromName != null) cfg.setFromName(fromName);
        if (tlsMode != null) cfg.setTlsMode(tlsMode);
        if (dailyQuota != null) cfg.setDailyQuota(dailyQuota);
        cfg.setUpdatedAt(Instant.now());
        SystemEmailConfig saved = repo.save(cfg);
        cachedSender = null; // invalidate cache
        return saved;
    }

    // ── Test connection ────────────────────────────────────────────────────

    @Transactional
    public void testarConexao() {
        Optional<SystemEmailConfig> cfgOpt = repo.findFirst();
        if (cfgOpt.isEmpty()) {
            throw BusinessException.unprocessable(
                    "EMAIL_NOT_CONFIGURED", "Conta de e-mail não configurada.");
        }
        SystemEmailConfig cfg = cfgOpt.get();
        String password = cipher.decrypt(cfg.getPasswordEnc(), cfg.getPasswordNonce());
        try {
            smtpHandshake(cfg, password);
            cfg.setFalhasAuthCount(0);
            cfg.setStatus("ATIVA");
            cfg.setUpdatedAt(Instant.now());
            repo.save(cfg);
            cachedSender = null;
        } catch (jakarta.mail.AuthenticationFailedException e) {
            registrarFalhaAuth(cfg);
            throw BusinessException.unprocessable(
                    "SMTP_AUTH_FAILED", "Autenticação SMTP falhou: " + e.getMessage());
        } catch (Exception e) {
            throw BusinessException.unprocessable(
                    "SMTP_CONNECTION_FAILED", "Conexão SMTP falhou: " + e.getMessage());
        }
    }

    // ── Circuit breaker ────────────────────────────────────────────────────

    @Transactional
    public void registrarFalhaAuth() {
        repo.findFirst().ifPresent(this::registrarFalhaAuth);
    }

    private void registrarFalhaAuth(SystemEmailConfig cfg) {
        Instant now = Instant.now();
        boolean withinWindow =
                cfg.getUltimaFalhaEm() != null
                        && Duration.between(cfg.getUltimaFalhaEm(), now)
                                        .compareTo(CIRCUIT_BREAKER_WINDOW)
                                < 0;
        int newCount = withinWindow ? cfg.getFalhasAuthCount() + 1 : 1;
        cfg.setFalhasAuthCount(newCount);
        cfg.setUltimaFalhaEm(now);
        if (newCount >= CIRCUIT_BREAKER_THRESHOLD) {
            cfg.setStatus("FALHA_AUTH");
            log.warn("system.email.circuit_breaker.open falhas={}", newCount);
            eventPublisher.publishEvent(new EmailAuthFalhouEvent(cfg.getFromAddress()));
        }
        cfg.setUpdatedAt(now);
        repo.save(cfg);
        cachedSender = null;
    }

    // ── SMTP handshake util ────────────────────────────────────────────────

    private void smtpHandshake(SystemEmailConfig cfg, String password) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.host", cfg.getHost());
        props.put("mail.smtp.port", String.valueOf(cfg.getPort()));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.connectiontimeout", "10000");
        if ("SSL".equalsIgnoreCase(cfg.getTlsMode())) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        Session session = Session.getInstance(props);
        jakarta.mail.Transport transport = session.getTransport("smtp");
        try {
            transport.connect(cfg.getHost(), cfg.getPort(), cfg.getUsername(), password);
        } finally {
            if (transport.isConnected()) transport.close();
        }
    }
}
