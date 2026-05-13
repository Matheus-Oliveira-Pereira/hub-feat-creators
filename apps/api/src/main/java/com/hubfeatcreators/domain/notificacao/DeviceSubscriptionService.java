package com.hubfeatcreators.domain.notificacao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeviceSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(DeviceSubscriptionService.class);

    private final DeviceSubscriptionRepository repo;

    public DeviceSubscriptionService(DeviceSubscriptionRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public DeviceSubscription register(
            UUID userId, String userTipo, String canal, String token, String plataforma) {
        return repo.findByToken(token)
                .map(
                        existing -> {
                            existing.setAtiva(true);
                            existing.setUltimoUso(Instant.now());
                            log.info(
                                    "device.register.reactivated userId={} canal={}",
                                    userId,
                                    canal);
                            return repo.save(existing);
                        })
                .orElseGet(
                        () -> {
                            DeviceSubscription sub =
                                    new DeviceSubscription(
                                            userId, userTipo, canal, token, plataforma);
                            log.info("device.register.new userId={} canal={}", userId, canal);
                            return repo.save(sub);
                        });
    }

    @Transactional
    public void unregister(String token) {
        repo.findByToken(token)
                .ifPresent(
                        sub -> {
                            sub.setAtiva(false);
                            repo.save(sub);
                            log.info(
                                    "device.unregister userId={} canal={}",
                                    sub.getUserId(),
                                    sub.getCanal());
                        });
    }

    @Transactional(readOnly = true)
    public List<DeviceSubscription> findActive(UUID userId) {
        return repo.findByUserIdAndAtivaTrue(userId);
    }
}
