package com.hubfeatcreators.onboarding;

import com.hubfeatcreators.domain.onboarding.LoginAttempt;
import com.hubfeatcreators.domain.onboarding.LoginAttemptRepository;
import com.hubfeatcreators.domain.onboarding.LoginLockoutService;
import com.hubfeatcreators.infra.web.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginLockoutServiceTest {

    @Mock LoginAttemptRepository repo;
    @InjectMocks LoginLockoutService service;

    @Test
    void checkLockout_noRecord_passes() {
        when(repo.findByKey("test@e.com")).thenReturn(Optional.empty());
        service.checkLockout("test@e.com"); // no exception
    }

    @Test
    void checkLockout_activeLock_throws429() {
        LoginAttempt locked = new LoginAttempt("test@e.com");
        locked.setCount(5);
        locked.setLockedUntil(Instant.now().plus(25, ChronoUnit.MINUTES));
        locked.setUpdatedAt(Instant.now());

        when(repo.findByKey("test@e.com")).thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> service.checkLockout("test@e.com"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void checkLockout_expiredLock_passes() {
        LoginAttempt expired = new LoginAttempt("test@e.com");
        expired.setLockedUntil(Instant.now().minus(1, ChronoUnit.HOURS));
        expired.setUpdatedAt(Instant.now());

        when(repo.findByKey("test@e.com")).thenReturn(Optional.of(expired));
        service.checkLockout("test@e.com"); // no exception
    }

    @Test
    void recordFailure_incrementsCount() {
        when(repo.findByKey("k")).thenReturn(Optional.empty());
        service.recordFailure("k");
        verify(repo).save(argThat(a -> a.getCount() == 1 && a.getLockedUntil() == null));
    }

    @Test
    void recordFailure_5thAttempt_setsLock() {
        LoginAttempt attempt = new LoginAttempt("k");
        attempt.setCount(4);
        attempt.setUpdatedAt(Instant.now());

        when(repo.findByKey("k")).thenReturn(Optional.of(attempt));
        service.recordFailure("k");
        verify(repo).save(argThat(a -> a.getLockedUntil() != null));
    }

    @Test
    void clearOnSuccess_deletesRecord() {
        service.clearOnSuccess("k");
        verify(repo).clearByKey("k");
    }
}
