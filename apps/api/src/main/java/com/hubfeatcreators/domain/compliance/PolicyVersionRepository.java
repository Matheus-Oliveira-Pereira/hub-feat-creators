package com.hubfeatcreators.domain.compliance;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PolicyVersionRepository extends JpaRepository<PolicyVersion, String> {
    @Query("SELECT p FROM PolicyVersion p ORDER BY p.vigenteDe DESC LIMIT 1")
    Optional<PolicyVersion> findLatest();
}
