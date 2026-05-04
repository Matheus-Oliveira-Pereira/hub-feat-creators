package com.hubfeatcreators.infra.job;

import com.hubfeatcreators.domain.whatsapp.WhatsappService;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component("WHATSAPP_SEND")
public class WhatsappSendJobHandler implements JobHandler {

    private final WhatsappService whatsappService;

    public WhatsappSendJobHandler(WhatsappService whatsappService) {
        this.whatsappService = whatsappService;
    }

    @Override
    public void handle(Job job) {
        String envioId = (String) job.getPayload().get("envioId");
        if (envioId == null) throw new IllegalArgumentException("envioId ausente no payload do job");
        whatsappService.processEnvio(UUID.fromString(envioId));
    }
}
