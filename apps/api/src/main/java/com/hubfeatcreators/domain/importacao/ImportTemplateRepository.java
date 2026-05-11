package com.hubfeatcreators.domain.importacao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportTemplateRepository extends JpaRepository<ImportTemplate, UUID> {

    List<ImportTemplate> findByAssessoriaIdAndEntidadeAndDeletedAtIsNull(
            UUID assessoriaId, String entidade);

    List<ImportTemplate> findByAssessoriaIdAndDeletedAtIsNull(UUID assessoriaId);

    Optional<ImportTemplate> findByIdAndAssessoriaIdAndDeletedAtIsNull(UUID id, UUID assessoriaId);
}
