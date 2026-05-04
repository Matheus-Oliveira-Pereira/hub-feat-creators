package com.hubfeatcreators.domain.compliance;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DsrTokenRepository extends JpaRepository<DsrToken, UUID> {
    Optional<DsrToken> findByTokenHash(String tokenHash);
}
