package com.hubfeatcreators.domain.email;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, UUID> {
    List<EmailTemplate> findByAssessoriaId(UUID assessoriaId);

    Optional<EmailTemplate> findByIdAndAssessoriaId(UUID id, UUID assessoriaId);
}
