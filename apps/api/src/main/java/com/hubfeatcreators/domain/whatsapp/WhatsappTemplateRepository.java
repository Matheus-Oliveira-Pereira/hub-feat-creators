package com.hubfeatcreators.domain.whatsapp;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsappTemplateRepository extends JpaRepository<WhatsappTemplate, UUID> {
    List<WhatsappTemplate> findByAssessoriaId(UUID assessoriaId);
    List<WhatsappTemplate> findByAccountIdAndStatus(UUID accountId, String status);
    Optional<WhatsappTemplate> findByIdAndAssessoriaId(UUID id, UUID assessoriaId);
    List<WhatsappTemplate> findByStatus(String status);
}
