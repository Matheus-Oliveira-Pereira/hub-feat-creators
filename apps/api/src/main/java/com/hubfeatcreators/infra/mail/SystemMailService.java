package com.hubfeatcreators.infra.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** Sends system transactional emails (verify, reset, invite) via default Spring Mail sender. */
@Service
public class SystemMailService {

    private static final Logger log = LoggerFactory.getLogger(SystemMailService.class);

    private final JavaMailSender mailSender;

    public SystemMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendVerifyEmail(String to, String verifyUrl) {
        send(to,
                "Verifique seu e-mail — feat. creators",
                "Clique no link para verificar sua conta:\n\n" + verifyUrl +
                "\n\nO link expira em 24 horas.");
    }

    @Async
    public void sendPasswordReset(String to, String resetUrl) {
        send(to,
                "Redefinição de senha — feat. creators",
                "Clique no link para redefinir sua senha:\n\n" + resetUrl +
                "\n\nO link expira em 1 hora. Se não solicitou, ignore este e-mail.");
    }

    @Async
    public void sendInvite(String to, String assessoriaNome, String inviteUrl) {
        send(to,
                "Convite para " + assessoriaNome + " — feat. creators",
                "Você foi convidado para entrar em " + assessoriaNome + ".\n\n" +
                "Clique no link para aceitar o convite:\n\n" + inviteUrl +
                "\n\nO convite expira em 7 dias.");
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("system.mail.error to={} subject={} error={}", to, subject, e.getMessage());
        }
    }
}
