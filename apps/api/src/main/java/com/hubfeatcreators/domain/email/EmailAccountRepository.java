package com.hubfeatcreators.domain.email;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailAccountRepository extends JpaRepository<EmailAccount, UUID> {

    List<EmailAccount> findByAssessoriaId(UUID assessoriaId);

    Optional<EmailAccount> findByIdAndAssessoriaId(UUID id, UUID assessoriaId);

    @Query(
            "SELECT COUNT(e) FROM EmailEnvio e WHERE e.accountId = :accountId "
                    + "AND e.status IN ('ENVIADO','ENFILEIRADO') "
                    + "AND e.createdAt >= :inicioDia")
    long countEnviosDia(
            @Param("accountId") UUID accountId, @Param("inicioDia") java.time.Instant inicioDia);
}
