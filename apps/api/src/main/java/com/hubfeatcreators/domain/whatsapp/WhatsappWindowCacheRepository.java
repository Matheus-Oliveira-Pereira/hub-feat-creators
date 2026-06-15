package com.hubfeatcreators.domain.whatsapp;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsappWindowCacheRepository extends JpaRepository<WhatsappWindowCache, String> {
    Optional<WhatsappWindowCache> findByE164(String e164);
}
