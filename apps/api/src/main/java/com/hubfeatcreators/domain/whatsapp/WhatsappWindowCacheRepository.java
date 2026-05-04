package com.hubfeatcreators.domain.whatsapp;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsappWindowCacheRepository
        extends JpaRepository<WhatsappWindowCache, WhatsappWindowCache.WindowKey> {
    Optional<WhatsappWindowCache> findByIdAssessoriaIdAndIdE164(UUID assessoriaId, String e164);
}
