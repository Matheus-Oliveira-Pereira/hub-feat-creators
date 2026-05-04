package com.hubfeatcreators.domain.whatsapp;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsappOptoutRepository extends JpaRepository<WhatsappOptout, UUID> {
    boolean existsByAssessoriaIdAndE164IgnoreCase(UUID assessoriaId, String e164);
    Optional<WhatsappOptout> findByAssessoriaIdAndE164IgnoreCase(UUID assessoriaId, String e164);
}
