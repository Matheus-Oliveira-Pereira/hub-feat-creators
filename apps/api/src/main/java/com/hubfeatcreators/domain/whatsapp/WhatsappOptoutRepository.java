package com.hubfeatcreators.domain.whatsapp;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsappOptoutRepository extends JpaRepository<WhatsappOptout, UUID> {
    boolean existsByE164IgnoreCase(String e164);

    Optional<WhatsappOptout> findByE164IgnoreCase(String e164);
}
