package com.hubfeatcreators.domain.whatsapp;

import com.hubfeatcreators.infra.web.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsappTemplateService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappTemplateService.class);

    private final WhatsappTemplateRepository templateRepo;
    private final WhatsappAccountRepository accountRepo;
    private final WhatsappAccountService accountService;
    private final MetaApiClient meta;

    public WhatsappTemplateService(WhatsappTemplateRepository templateRepo,
            WhatsappAccountRepository accountRepo,
            WhatsappAccountService accountService,
            MetaApiClient meta) {
        this.templateRepo = templateRepo;
        this.accountRepo = accountRepo;
        this.accountService = accountService;
        this.meta = meta;
    }

    @Transactional(readOnly = true)
    public List<WhatsappTemplate> list(UUID assessoriaId) {
        return templateRepo.findByAssessoriaId(assessoriaId);
    }

    @Transactional
    public WhatsappTemplate create(UUID assessoriaId, UUID accountId, String nome,
            String idioma, String categoria, String corpo, String[] variaveis) {
        accountService.requireAccount(assessoriaId, accountId);
        var template = new WhatsappTemplate(assessoriaId, accountId, nome, idioma, categoria, corpo, variaveis);
        return templateRepo.save(template);
    }

    @Transactional
    public WhatsappTemplate submit(UUID assessoriaId, UUID templateId) {
        WhatsappTemplate template = require(assessoriaId, templateId);
        WhatsappAccount account = accountService.requireAccount(assessoriaId, template.getAccountId());
        String token = accountService.decryptToken(account);

        String metaId = meta.submitTemplate(account.getWabaId(), token,
                template.getNome(), template.getIdioma(),
                template.getCategoria(), template.getCorpo());

        template.setMetaTemplateId(metaId);
        template.setStatus("PENDING");
        template.setSubmetidoEm(Instant.now());
        return templateRepo.save(template);
    }

    /** Polls PENDING templates every 15 minutes. */
    @Scheduled(fixedDelay = 900_000)
    @Transactional
    public void pollPendingTemplates() {
        List<WhatsappTemplate> pending = templateRepo.findByStatus("PENDING");
        for (WhatsappTemplate t : pending) {
            if (t.getMetaTemplateId() == null) continue;
            try {
                WhatsappAccount account = accountRepo.findById(t.getAccountId()).orElse(null);
                if (account == null) continue;
                String token = accountService.decryptToken(account);
                String status = meta.getTemplateStatus(account.getWabaId(), token, t.getMetaTemplateId());
                if (!status.equals(t.getStatus())) {
                    t.setStatus(status);
                    t.setAtualizadoEm(Instant.now());
                    templateRepo.save(t);
                }
            } catch (Exception e) {
                log.warn("Template poll failed for {}: {}", t.getId(), e.getMessage());
            }
        }
    }

    private WhatsappTemplate require(UUID assessoriaId, UUID id) {
        return templateRepo.findByIdAndAssessoriaId(id, assessoriaId)
                .orElseThrow(BusinessException::notFound);
    }
}
