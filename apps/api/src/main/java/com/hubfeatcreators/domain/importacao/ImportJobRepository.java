package com.hubfeatcreators.domain.importacao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImportJobRepository extends JpaRepository<ImportJob, UUID> {

    Optional<ImportJob> findByIdAndAssessoriaId(UUID id, UUID assessoriaId);

    Page<ImportJob> findByAssessoriaIdOrderByCreatedAtDesc(UUID assessoriaId, Pageable pageable);

    @Query(
            """
      SELECT COUNT(j) FROM ImportJob j
      WHERE j.assessoriaId = :assessoriaId
        AND j.status = 'EXECUTANDO'
      """)
    long countExecutandoByAssessoria(@Param("assessoriaId") UUID assessoriaId);

    @Query(
            """
      SELECT COUNT(j) FROM ImportJob j
      WHERE j.usuarioId = :usuarioId
        AND j.status = 'EXECUTANDO'
      """)
    long countExecutandoByUsuario(@Param("usuarioId") UUID usuarioId);

    @Query(
            """
      SELECT j FROM ImportJob j
      WHERE j.cancelableBatchId = :batchId
      """)
    List<ImportJob> findByBatchId(@Param("batchId") UUID batchId);
}
