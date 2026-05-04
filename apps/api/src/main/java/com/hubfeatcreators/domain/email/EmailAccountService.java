package com.hubfeatcreators.domain.email;

import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.web.BusinessException;
import jakarta.mail.Session;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailAccountService {

    private static final int CIRCUIT_BREAKER_THRESHOLD = 3;
    private static final Duration CIRCUIT_BREAKER_WINDOW = Duration.ofMinutes(10);

    private final EmailAccountRepository repo;
    private final EmailCipherService cipher;

    public EmailAccountService(EmailAccountRepository repo, EmailCipherService cipher) {
        this.repo = repo;
        this.cipher = cipher;
    }

    @Transactional(readOnly = true)
    public List<EmailAccount> listar(AuthPrincipal principal) {
        return repo.findByAssessoriaId(principal.assessoriaId());
    }

    @Transactional(readOnly = true)
    public EmailAccount buscar(AuthPrincipal principal, UUID id) {
        return repo.findByIdAndAssessoriaId(id, principal.assessoriaId())
                .orElseThrow(() -> BusinessException.notFound("EMAIL_ACCOUNT"));
    }

    @Transactional
    public EmailAccount criar(
            AuthPrincipal principal,
            String nome,
            String host,
            int port,
            String username,
            String password,
            String fromAddress,
            String fromName,
            TlsMode tlsMode,
            int dailyQuota) {
        EmailCipherService.Encrypted enc = cipher.encrypt(password);
        EmailAccount account =
                new EmailAccount(
                        principal.assessoriaId(),
                        nome,
                        host,
                        port,
                        username,
                        enc.ciphertext(),
                        enc.nonce(),
                        fromAddress,
                        fromName,
                        tlsMode,
                        dailyQuota);
        return repo.save(account);
    }

    @Transactional
    public EmailAccount atualizar(
            AuthPrincipal principal,
            UUID id,
            String nome,
            String host,
            Integer port,
            String username,
            String password,
            String fromAddress,
            String fromName,
            TlsMode tlsMode,
            Integer dailyQuota) {
        EmailAccount account = buscar(principal, id);
        if (nome != null) account.setNome(nome);
        if (host != null) account.setHost(host);
        if (port != null) account.setPort(port);
        if (username != null) account.setUsername(username);
        if (password != null) {
            EmailCipherService.Encrypted enc = cipher.encrypt(password);
            account.setPasswordEncrypted(enc.ciphertext());
            account.setPasswordNonce(enc.nonce());
        }
        if (fromAddress != null) account.setFromAddress(fromAddress);
        if (fromName != null) account.setFromName(fromName);
        if (tlsMode != null) account.setTlsMode(tlsMode);
        if (dailyQuota != null) account.setDailyQuota(dailyQuota);
        account.setUpdatedAt(Instant.now());
        return repo.save(account);
    }

    @Transactional
    public void deletar(AuthPrincipal principal, UUID id) {
        EmailAccount account = buscar(principal, id);
        account.setDeletedAt(Instant.now());
        repo.save(account);
    }

    /**
     * Tests SMTP connectivity and authentication. On success: resets falhasAuthCount. On auth
     * failure: increments counter; after CIRCUIT_BREAKER_THRESHOLD → FALHA_AUTH.
     */
    @Transactional
    public void testarConexao(AuthPrincipal principal, UUID id) {
        EmailAccount account = buscar(principal, id);
        String password =
                cipher.decrypt(account.getPasswordEncrypted(), account.getPasswordNonce());
        try {
            smtpHandshake(account, password);
            account.setFalhasAuthCount(0);
            account.setStatus(EmailAccountStatus.ATIVA);
            account.setUpdatedAt(Instant.now());
            repo.save(account);
        } catch (jakarta.mail.AuthenticationFailedException e) {
            registrarFalhaAuth(account);
            throw BusinessException.unprocessable("SMTP_AUTH_FAILED", "Autenticação SMTP falhou");
        } catch (Exception e) {
            throw BusinessException.unprocessable("SMTP_CONNECTION_FAILED", "Conexão SMTP falhou");
        }
    }

    /** Called by send job when authentication fails mid-send. */
    @Transactional
    public void registrarFalhaAuthById(UUID accountId) {
        repo.findById(accountId).ifPresent(this::registrarFalhaAuth);
    }

    private void registrarFalhaAuth(EmailAccount account) {
        Instant now = Instant.now();
        boolean withinWindow =
                account.getUltimaFalhaEm() != null
                        && Duration.between(account.getUltimaFalhaEm(), now)
                                        .compareTo(CIRCUIT_BREAKER_WINDOW)
                                < 0;
        int newCount = withinWindow ? account.getFalhasAuthCount() + 1 : 1;
        account.setFalhasAuthCount(newCount);
        account.setUltimaFalhaEm(now);
        if (newCount >= CIRCUIT_BREAKER_THRESHOLD) {
            account.setStatus(EmailAccountStatus.FALHA_AUTH);
        }
        account.setUpdatedAt(now);
        repo.save(account);
    }

    private void smtpHandshake(EmailAccount account, String password) throws Exception {
        Properties props = new Properties();
        props.put("mail.smtp.host", account.getHost());
        props.put("mail.smtp.port", String.valueOf(account.getPort()));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.connectiontimeout", "10000");

        if (account.getTlsMode() == TlsMode.SSL) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }

        Session session = Session.getInstance(props);
        jakarta.mail.Transport transport = session.getTransport("smtp");
        try {
            transport.connect(
                    account.getHost(), account.getPort(), account.getUsername(), password);
        } finally {
            if (transport.isConnected()) transport.close();
        }
    }
}
