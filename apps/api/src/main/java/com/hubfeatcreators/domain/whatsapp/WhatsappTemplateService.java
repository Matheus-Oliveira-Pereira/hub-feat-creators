package com.hubfeatcreators.domain.whatsapp;

import com.hubfeatcreators.infra.web.BusinessException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsappTemplateService {

    private static final Logger log = LoggerFactory.getLogger(WhatsappTemplateService.class);
    private static final int POLL_LIMIT = 50;

    private final WhatsappTemplateRepository templateRepo;
    private final WhatsappAccountRepository accountRepo;
    private final WhatsappAccountService accountService;
    private final MetaApiClient meta;

    public WhatsappTemplateService(
            WhatsappTemplateRepository templateRepo,
            WhatsappAccountRepository accountRepo,
            WhatsappAccountService accountService,
            MetaApiClient meta) {
        this.templateRepo = templateRepo;
        this.accountRepo = accountRepo;
        this.accountService = accountService;
        this.meta = meta;
    }

    @Transactional(readOnly = true)
    public List<WhatsappTemplate> list() {
        return templateRepo.findAllTemplates();
    }

    @Transactional
    public WhatsappTemplate create(
            UUID accountId,
            String nome,
            String idioma,
            String categoria,
            String corpo,
            String[] variaveis) {
        accountService.requireAccount(accountId);
        var template = new WhatsappTemplate(accountId, nome, idioma, categoria, corpo, variaveis);
        return templateRepo.save(template);
    }

    @Transactional
    public WhatsappTemplate submit(UUID templateId) {
        WhatsappTemplate template = require(templateId);
        WhatsappAccount account = accountService.requireAccount(template.getAccountId());
        String token = accountService.decryptToken(account);

        String metaId =
                meta.submitTemplate(
                        account.getWabaId(),
                        token,
                        template.getNome(),
                        template.getIdioma(),
                        template.getCategoria(),
                        template.getCorpo());

        template.setMetaTemplateId(metaId);
        template.setStatus("PENDING");
        template.setSubmetidoEm(Instant.now());
        return templateRepo.save(template);
    }

    /** Polls PENDING templates every 15 minutes. Bounded to POLL_LIMIT per cycle. */
    @Scheduled(fixedDelay = 900_000)
    @SchedulerLock(
            name = "whatsapp_template_poll",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT1M")
    @Transactional
    public void pollPendingTemplates() {
        List<WhatsappTemplate> pending =
                templateRepo.findByStatusAndMetaTemplateIdIsNotNull(
                        "PENDING", PageRequest.of(0, POLL_LIMIT));
        if (pending.isEmpty()) return;

        // Batch-load accounts to avoid N+1
        Set<UUID> accountIds =
                pending.stream().map(WhatsappTemplate::getAccountId).collect(Collectors.toSet());
        Map<UUID, WhatsappAccount> accounts =
                accountRepo.findAllById(accountIds).stream()
                        .collect(Collectors.toMap(WhatsappAccount::getId, a -> a));

        for (WhatsappTemplate t : pending) {
            try {
                WhatsappAccount account = accounts.get(t.getAccountId());
                if (account == null) continue;
                String token = accountService.decryptToken(account);
                String status =
                        meta.getTemplateStatus(account.getWabaId(), token, t.getMetaTemplateId());
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

    private WhatsappTemplate require(UUID id) {
        return templateRepo.findById(id).orElseThrow(BusinessException::notFound);
    }
}
