package com.hubfeatcreators.infra.job;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Envia notificação LGPD ao influenciador informando que seus dados foram cadastrados em um sistema
 * de assessoria (base legal: legítimo interesse do controlador). Idempotente: job com mesma chave
 * não é reenfileirado.
 */
@Component("INFLUENCIADOR_LGPD_NOTIFY")
public class LgpdNotifyJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(LgpdNotifyJobHandler.class);

    private final JavaMailSender mailSender;

    public LgpdNotifyJobHandler(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void handle(Job job) {
        Map<String, Object> payload = job.getPayload();
        String email = (String) payload.get("email");
        String influenciadorId = (String) payload.get("influenciadorId");

        if (email == null || email.isBlank()) {
            log.info("lgpd.notify.skip influenciadorId={} motivo=sem_email", influenciadorId);
            return;
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("Informação sobre tratamento dos seus dados");
        msg.setText(
                """
        Olá,

        Seus dados profissionais (nome, perfis em redes sociais, nicho de atuação)
        foram cadastrados em uma plataforma de gestão de assessorias de comunicação.

        Base legal: legítimo interesse do controlador (Art. 7º, IX, LGPD).
        Finalidade: gerenciamento de relacionamentos comerciais entre marcas e influenciadores.

        Você tem o direito de:
        - Confirmar a existência do tratamento
        - Solicitar acesso aos seus dados
        - Solicitar a exclusão dos seus dados

        Para exercer seus direitos, responda este e-mail.

        Atenciosamente,
        HUB Feat Creator
        """);

        mailSender.send(msg);
        log.info("lgpd.notify.sent email={} influenciadorId={}", email, influenciadorId);
    }
}
