package com.hubfeatcreators.domain.convite;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConviteRepository extends JpaRepository<Convite, UUID> {
    Optional<Convite> findByToken(String token);

    List<Convite> findByAssessoriaIdAndUsedAtIsNullAndExpiresAtAfter(UUID assessoriaId, Instant now);

    List<Convite> findByAssessoriaIdAndUsedAtIsNull(UUID assessoriaId);
}
