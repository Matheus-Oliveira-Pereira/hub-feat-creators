package com.hubfeatcreators.domain.importacao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportTemplateRepository extends JpaRepository<ImportTemplate, UUID> {

    List<ImportTemplate> findByEntidadeAndDeletedAtIsNull(String entidade);

    List<ImportTemplate> findByDeletedAtIsNull();

    Optional<ImportTemplate> findByIdAndDeletedAtIsNull(UUID id);
}
