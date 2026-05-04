package com.hubfeatcreators.domain.whatsapp;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsappEventoInboundRepository extends JpaRepository<WhatsappEventoInbound, UUID> {
    Optional<WhatsappEventoInbound> findByWamid(String wamid);
    boolean existsByWamid(String wamid);
}
