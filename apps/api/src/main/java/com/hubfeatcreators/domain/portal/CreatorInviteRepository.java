package com.hubfeatcreators.domain.portal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorInviteRepository extends JpaRepository<CreatorInvite, UUID> {

    Optional<CreatorInvite> findByTokenHash(String tokenHash);

    List<CreatorInvite> findByInfluenciadorIdAndAssessoriaIdAndAceitoEmIsNull(
            UUID influenciadorId, UUID assessoriaId);

    boolean existsByInfluenciadorIdAndAssessoriaIdAndAceitoEmIsNull(
            UUID influenciadorId, UUID assessoriaId);
}
