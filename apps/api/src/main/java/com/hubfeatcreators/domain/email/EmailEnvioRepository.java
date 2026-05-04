package com.hubfeatcreators.domain.email;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailEnvioRepository extends JpaRepository<EmailEnvio, UUID> {

    Optional<EmailEnvio> findByIdAndAssessoriaId(UUID id, UUID assessoriaId);

    Optional<EmailEnvio> findByAssessoriaIdAndIdempotencyKey(
            UUID assessoriaId, UUID idempotencyKey);

    @Query(
            "SELECT e FROM EmailEnvio e WHERE e.assessoriaId = :assessoriaId "
                    + "AND (:contextoKey IS NULL OR CAST(e.contexto AS string) LIKE CONCAT('%', :contextoKey, '%'))"
                    + " ORDER BY e.createdAt DESC")
    Page<EmailEnvio> findByAssessoriaId(
            @Param("assessoriaId") UUID assessoriaId,
            @Param("contextoKey") String contextoKey,
            Pageable pageable);
}
