package com.hubfeatcreators.domain.admin;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformFeatureFlagService {

    private static final Logger log = LoggerFactory.getLogger(PlatformFeatureFlagService.class);

    private final PlatformFeatureFlagRepository repo;
    private final Environment env;

    // Cache TTL 30s per ADR-017
    private final Cache<String, Boolean> cache =
            Caffeine.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).maximumSize(200).build();

    public PlatformFeatureFlagService(PlatformFeatureFlagRepository repo, Environment env) {
        this.repo = repo;
        this.env = env;
    }

    /** DB first → cache; fallback to env var if DB miss or error. */
    public boolean isEnabled(String key) {
        Boolean cached = cache.getIfPresent(key);
        if (cached != null) return cached;

        try {
            var flag = repo.findByKey(key);
            if (flag.isPresent()) {
                boolean val = flag.get().isEnabled();
                cache.put(key, val);
                return val;
            }
        } catch (Exception e) {
            log.warn("feature_flag.db.error key={}: {}", key, e.getMessage());
        }

        // Fallback: env var (e.g. FEATURE_PORTAL_ENABLED → default false)
        String envVal = env.getProperty(key, "false");
        return Boolean.parseBoolean(envVal);
    }

    @Transactional
    public PlatformFeatureFlag toggle(String key, boolean enabled, UUID adminId) {
        PlatformFeatureFlag flag =
                repo.findByKey(key).orElse(new PlatformFeatureFlag(key, enabled, adminId));
        flag.setEnabled(enabled);
        flag.setUpdatedBy(adminId);
        flag.setUpdatedAt(Instant.now());
        PlatformFeatureFlag saved = repo.save(flag);
        cache.invalidate(key);
        return saved;
    }

    public List<PlatformFeatureFlag> findAll() {
        return repo.findAll();
    }
}
