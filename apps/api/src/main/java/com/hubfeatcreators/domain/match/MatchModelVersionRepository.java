package com.hubfeatcreators.domain.match;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MatchModelVersionRepository extends JpaRepository<MatchModelVersion, String> {

    @Query(
            "SELECT m FROM MatchModelVersion m WHERE m.desativadaEm IS NULL ORDER BY m.ativadaEm DESC LIMIT 1")
    Optional<MatchModelVersion> findActiveVersion();
}
