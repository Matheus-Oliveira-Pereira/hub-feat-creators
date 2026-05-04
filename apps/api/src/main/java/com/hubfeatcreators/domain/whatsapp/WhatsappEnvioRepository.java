package com.hubfeatcreators.domain.whatsapp;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsappEnvioRepository extends JpaRepository<WhatsappEnvio, UUID> {
    Optional<WhatsappEnvio> findByIdempotencyKey(UUID key);
    Optional<WhatsappEnvio> findByWamid(String wamid);
    Optional<WhatsappEnvio> findByIdAndAssessoriaId(UUID id, UUID assessoriaId);
}
