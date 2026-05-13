package com.hubfeatcreators.domain.portal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorUserRepository extends JpaRepository<CreatorUser, UUID> {

    Optional<CreatorUser> findByEmail(String email);

    Optional<CreatorUser> findByInfluenciadorIdAndAssessoriaId(
            UUID influenciadorId, UUID assessoriaId);

    boolean existsByInfluenciadorIdAndAssessoriaId(UUID influenciadorId, UUID assessoriaId);
}
