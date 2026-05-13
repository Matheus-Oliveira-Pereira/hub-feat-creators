package com.hubfeatcreators.domain.social;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class OAuthStateStore {

    private static final long TTL_SECONDS = 600;

    public record OAuthPending(UUID influenciadorId, UUID assessoriaId, Instant createdAt) {}

    private final Map<String, OAuthPending> store = new ConcurrentHashMap<>();

    public String generate(UUID influenciadorId, UUID assessoriaId) {
        evictExpired();
        String state = UUID.randomUUID().toString();
        store.put(state, new OAuthPending(influenciadorId, assessoriaId, Instant.now()));
        return state;
    }

    public Optional<OAuthPending> consume(String state) {
        OAuthPending pending = store.remove(state);
        if (pending == null) return Optional.empty();
        if (Instant.now().isAfter(pending.createdAt().plusSeconds(TTL_SECONDS)))
            return Optional.empty();
        return Optional.of(pending);
    }

    private void evictExpired() {
        Instant cutoff = Instant.now().minusSeconds(TTL_SECONDS);
        Iterator<Map.Entry<String, OAuthPending>> it = store.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().createdAt().isBefore(cutoff)) it.remove();
        }
    }
}
