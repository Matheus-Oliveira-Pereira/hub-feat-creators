package com.hubfeatcreators.domain.whatsapp;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface WhatsappTemplateRepository extends JpaRepository<WhatsappTemplate, UUID> {

    @Query("SELECT t FROM WhatsappTemplate t ORDER BY t.nome ASC")
    List<WhatsappTemplate> findAllTemplates();

    List<WhatsappTemplate> findByAccountIdAndStatus(UUID accountId, String status);

    List<WhatsappTemplate> findByStatusAndMetaTemplateIdIsNotNull(String status, Pageable pageable);
}
