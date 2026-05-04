package com.hubfeatcreators.domain.email;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailOptoutRepository extends JpaRepository<EmailOptout, UUID> {
    boolean existsByAssessoriaIdAndEmailIgnoreCase(UUID assessoriaId, String email);

    Optional<EmailOptout> findByAssessoriaIdAndEmailIgnoreCase(UUID assessoriaId, String email);
}
