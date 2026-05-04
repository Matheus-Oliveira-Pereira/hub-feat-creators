package com.hubfeatcreators.domain.onboarding;

import com.hubfeatcreators.infra.web.BusinessException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginLockoutService {

    private static final Logger log = LoggerFactory.getLogger(LoginLockoutService.class);
    private static final int MAX_ATTEMPTS = 5;
    private static final int WINDOW_MINUTES = 15;
    private static final int LOCK_MINUTES = 30;

    private final LoginAttemptRepository repo;

    public LoginLockoutService(LoginAttemptRepository repo) {
        this.repo = repo;
    }

    /** Throws BusinessException if key is locked. */
    @Transactional
    public void checkLockout(String key) {
        repo.findByKey(key).ifPresent(a -> {
            if (a.isLocked()) {
                log.warn("lockout.blocked key={}", key);
                throw BusinessException.tooManyRequests(
                        "ACCOUNT_LOCKED",
                        "Conta bloqueada por excesso de tentativas. Tente novamente em 30 minutos.");
            }
        });
    }

    /** Records a failed attempt. Locks if threshold reached. */
    @Transactional
    public void recordFailure(String key) {
        LoginAttempt attempt = repo.findByKey(key).orElseGet(() -> new LoginAttempt(key));

        // Reset window if last attempt was outside window
        if (attempt.getUpdatedAt() != null &&
                attempt.getUpdatedAt().isBefore(Instant.now().minus(WINDOW_MINUTES, ChronoUnit.MINUTES))) {
            attempt.setCount(0);
            attempt.setLockedUntil(null);
        }

        attempt.setCount(attempt.getCount() + 1);
        attempt.setUpdatedAt(Instant.now());

        if (attempt.getCount() >= MAX_ATTEMPTS) {
            attempt.setLockedUntil(Instant.now().plus(LOCK_MINUTES, ChronoUnit.MINUTES));
            log.warn("lockout.locked key={} count={}", key, attempt.getCount());
        }

        repo.save(attempt);
    }

    /** Clears lockout record on successful login. */
    @Transactional
    public void clearOnSuccess(String key) {
        repo.clearByKey(key);
    }
}
