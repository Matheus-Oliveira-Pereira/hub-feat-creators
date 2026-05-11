package com.hubfeatcreators.domain.relatorio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelatorioSalvoRepository extends JpaRepository<RelatorioSalvo, UUID> {

    List<RelatorioSalvo> findByAssessoriaIdOrderByCreatedAtDesc(UUID assessoriaId);

    Optional<RelatorioSalvo> findByIdAndAssessoriaId(UUID id, UUID assessoriaId);
}
