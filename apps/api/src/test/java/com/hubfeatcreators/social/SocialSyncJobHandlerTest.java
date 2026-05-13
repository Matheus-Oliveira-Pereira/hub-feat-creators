package com.hubfeatcreators.social;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.hubfeatcreators.domain.social.SocialService;
import com.hubfeatcreators.infra.job.Job;
import com.hubfeatcreators.infra.job.SocialSyncJobHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SocialSyncJobHandlerTest {

    @Mock SocialService socialService;
    @InjectMocks SocialSyncJobHandler handler;

    @Test
    void handle_calls_syncAccount() {
        UUID accountId = UUID.randomUUID();
        Job job = mockJob(Map.of("accountId", accountId.toString()));

        handler.handle(job);

        verify(socialService).syncAccount(accountId);
    }

    @Test
    void handle_throws_when_accountId_missing() {
        Job job = mockJob(new HashMap<>());

        assertThatThrownBy(() -> handler.handle(job))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accountId");
    }

    private Job mockJob(Map<String, Object> payload) {
        Job job = mock(Job.class);
        when(job.getPayload()).thenReturn(payload);
        return job;
    }
}
