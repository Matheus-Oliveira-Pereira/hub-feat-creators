package com.hubfeatcreators.domain.compliance;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyAceiteRepository extends JpaRepository<PolicyAceite, UUID> {
    Optional<PolicyAceite> findByUserIdAndVersao(UUID userId, String versao);
    boolean existsByUserIdAndVersao(UUID userId, String versao);
}
