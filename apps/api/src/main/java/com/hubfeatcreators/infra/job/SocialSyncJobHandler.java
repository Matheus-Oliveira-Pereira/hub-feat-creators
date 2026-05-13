package com.hubfeatcreators.infra.job;

import com.hubfeatcreators.domain.social.SocialService;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component("SOCIAL_SYNC")
public class SocialSyncJobHandler implements JobHandler {

    private final SocialService socialService;

    public SocialSyncJobHandler(SocialService socialService) {
        this.socialService = socialService;
    }

    @Override
    public void handle(Job job) {
        String accountId = (String) job.getPayload().get("accountId");
        if (accountId == null) throw new IllegalArgumentException("accountId ausente no payload");
        socialService.syncAccount(UUID.fromString(accountId));
    }
}
