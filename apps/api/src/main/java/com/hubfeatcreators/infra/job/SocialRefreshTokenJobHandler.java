package com.hubfeatcreators.infra.job;

import com.hubfeatcreators.domain.social.SocialService;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component("SOCIAL_REFRESH_TOKEN")
public class SocialRefreshTokenJobHandler implements JobHandler {

    private final SocialService socialService;

    public SocialRefreshTokenJobHandler(SocialService socialService) {
        this.socialService = socialService;
    }

    @Override
    public void handle(Job job) {
        String accountId = (String) job.getPayload().get("accountId");
        if (accountId == null) throw new IllegalArgumentException("accountId ausente no payload");
        socialService.refreshToken(UUID.fromString(accountId));
    }
}
