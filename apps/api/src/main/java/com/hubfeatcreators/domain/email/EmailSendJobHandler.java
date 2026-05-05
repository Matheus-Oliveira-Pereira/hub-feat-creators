package com.hubfeatcreators.domain.email;

import com.hubfeatcreators.domain.notificacao.events.EmailAuthFalhouEvent;
import com.hubfeatcreators.infra.job.Job;
import com.hubfeatcreators.infra.job.JobHandler;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Processes EMAIL_SEND jobs: builds MIME message per account config, sends via SMTP. */
@Component("EMAIL_SEND")
public class EmailSendJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(EmailSendJobHandler.class);

    private final EmailEnvioRepository envioRepo;
    private final EmailAccountRepository accountRepo;
    private final EmailAccountService accountService;
    private final EmailCipherService cipher;
    private final MeterRegistry meterRegistry;
    private final ApplicationEventPublisher eventPublisher;

    public EmailSendJobHandler(
            EmailEnvioRepository envioRepo,
            EmailAccountRepository accountRepo,
            EmailAccountService accountService,
            EmailCipherService cipher,
            MeterRegistry meterRegistry,
            ApplicationEventPublisher eventPublisher) {
        this.envioRepo = envioRepo;
        this.accountRepo = accountRepo;
        this.accountService = accountService;
        this.cipher = cipher;
        this.meterRegistry = meterRegistry;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public void handle(Job job) throws Exception {
        Map<String, Object> payload = job.getPayload();
        UUID envioId = UUID.fromString((String) payload.get("envioId"));

        EmailEnvio envio =
                envioRepo
                        .findById(envioId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "EmailEnvio not found: " + envioId));

        if (envio.getStatus() == EmailEnvioStatus.ENVIADO) {
            log.info("email.send.skip envioId={} motivo=already_sent", envioId);
            return;
        }

        EmailAccount account =
                accountRepo
                        .findById(envio.getAccountId())
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "EmailAccount not found: " + envio.getAccountId()));

        envio.setTentativas(envio.getTentativas() + 1);

        try {
            String password =
                    cipher.decrypt(account.getPasswordEncrypted(), account.getPasswordNonce());
            String smtpMessageId = sendViaSmtp(envio, account, password);

            envio.setStatus(EmailEnvioStatus.ENVIADO);
            envio.setSmtpMessageId(smtpMessageId);
            envio.setEnviadoEm(Instant.now());
            envioRepo.save(envio);

            Counter.builder("email_enviado_total").register(meterRegistry).increment();
            log.info("email.send.ok envioId={} smtpMessageId={}", envioId, smtpMessageId);

        } catch (AuthenticationFailedException e) {
            accountService.registrarFalhaAuthById(account.getId());
            envio.setStatus(EmailEnvioStatus.FALHOU);
            envio.setFalhaMotivo("SMTP_AUTH_FAILED: " + e.getMessage());
            envioRepo.save(envio);
            Counter.builder("email_falha_total")
                    .tag("reason", "auth")
                    .register(meterRegistry)
                    .increment();
            log.error("email.send.auth_fail envioId={} accountId={}", envioId, account.getId());
            eventPublisher.publishEvent(new EmailAuthFalhouEvent(
                    account.getAssessoriaId(), account.getId(), account.getFromAddress()));
            throw e;

        } catch (Exception e) {
            envio.setStatus(EmailEnvioStatus.FALHOU);
            envio.setFalhaMotivo(e.getMessage());
            envioRepo.save(envio);
            Counter.builder("email_falha_total")
                    .tag("reason", "smtp")
                    .register(meterRegistry)
                    .increment();
            log.error("email.send.error envioId={} msg={}", envioId, e.getMessage(), e);
            throw e;
        }
    }

    private String sendViaSmtp(EmailEnvio envio, EmailAccount account, String password)
            throws Exception {
        Properties props = buildSmtpProps(account);
        Session session = Session.getInstance(props);

        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(account.getFromAddress(), account.getFromName(), "UTF-8"));

        String toAddr =
                envio.getDestinatarioNome() != null
                        ? new InternetAddress(
                                        envio.getDestinatarioEmail(),
                                        envio.getDestinatarioNome(),
                                        "UTF-8")
                                .toString()
                        : envio.getDestinatarioEmail();
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddr));
        msg.setSubject(envio.getAssunto(), "UTF-8");
        msg.setHeader("List-Unsubscribe", "<" + buildUnsubscribeHeader(envio) + ">");
        msg.setHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");

        MimeBodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(envio.getCorpoHtmlRenderizado(), "text/html; charset=UTF-8");
        Multipart multipart = new MimeMultipart("alternative");
        multipart.addBodyPart(htmlPart);
        msg.setContent(multipart);
        msg.saveChanges();

        try (Transport transport = session.getTransport("smtp")) {
            transport.connect(
                    account.getHost(), account.getPort(), account.getUsername(), password);
            transport.sendMessage(msg, msg.getAllRecipients());
        }

        return msg.getMessageID();
    }

    private Properties buildSmtpProps(EmailAccount account) {
        Properties props = new Properties();
        props.put("mail.smtp.host", account.getHost());
        props.put("mail.smtp.port", String.valueOf(account.getPort()));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.timeout", "30000");
        props.put("mail.smtp.connectiontimeout", "15000");
        if (account.getTlsMode() == TlsMode.SSL) {
            props.put("mail.smtp.ssl.enable", "true");
        } else {
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        }
        return props;
    }

    private String buildUnsubscribeHeader(EmailEnvio envio) {
        String token =
                java.util.Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                (envio.getAssessoriaId() + ":" + envio.getDestinatarioEmail())
                                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return "/api/v1/email/unsubscribe?token=" + token;
    }
}
