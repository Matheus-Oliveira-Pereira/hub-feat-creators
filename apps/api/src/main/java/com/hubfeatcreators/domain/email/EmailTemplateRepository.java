package com.hubfeatcreators.domain.email;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {

    @Query("SELECT t FROM EmailTemplate t WHERE t.deletedAt IS NULL ORDER BY t.nome ASC")
    List<EmailTemplate> findAllActive();
}
