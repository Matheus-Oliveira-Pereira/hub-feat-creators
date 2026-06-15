package com.hubfeatcreators.domain.email;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailEnvioRepository extends JpaRepository<EmailEnvio, UUID> {

    Optional<EmailEnvio> findByIdempotencyKey(UUID idempotencyKey);

    @Query(
            "SELECT e FROM EmailEnvio e WHERE"
                    + " (:contextoKey IS NULL OR CAST(e.contexto AS string) LIKE CONCAT('%', :contextoKey, '%'))"
                    + " ORDER BY e.createdAt DESC")
    Page<EmailEnvio> findAllFiltered(@Param("contextoKey") String contextoKey, Pageable pageable);

    @Query(
            "SELECT COUNT(e) FROM EmailEnvio e WHERE e.createdAt >= :inicioDia"
                    + " AND e.status IN (com.hubfeatcreators.domain.email.EmailEnvioStatus.ENVIADO,"
                    + " com.hubfeatcreators.domain.email.EmailEnvioStatus.ENFILEIRADO)")
    long countEnviosDia(@Param("inicioDia") Instant inicioDia);
}
