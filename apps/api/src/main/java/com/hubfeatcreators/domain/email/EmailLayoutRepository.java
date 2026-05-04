package com.hubfeatcreators.domain.email;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailLayoutRepository extends JpaRepository<EmailLayout, UUID> {
    Optional<EmailLayout> findByAssessoriaId(UUID assessoriaId);
}
