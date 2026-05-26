package com.hubfeatcreators.domain.portal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorEntregavelRepository extends JpaRepository<CreatorEntregavel, UUID> {

    List<CreatorEntregavel> findByTarefaId(UUID tarefaId);

    List<CreatorEntregavel> findByCreatorUserId(UUID creatorUserId);
}
