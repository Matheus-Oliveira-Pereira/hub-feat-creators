package com.hubfeatcreators.domain.portal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorEntregavelRepository extends JpaRepository<CreatorEntregavel, UUID> {

    List<CreatorEntregavel> findByTarefaIdAndAssessoriaId(UUID tarefaId, UUID assessoriaId);

    Optional<CreatorEntregavel> findByIdAndAssessoriaId(UUID id, UUID assessoriaId);

    List<CreatorEntregavel> findByCreatorUserIdAndAssessoriaId(UUID creatorUserId, UUID assessoriaId);
}
