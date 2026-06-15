package com.hubfeatcreators.domain.email;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SystemEmailConfigRepository extends JpaRepository<SystemEmailConfig, UUID> {

    @Query("SELECT c FROM SystemEmailConfig c ORDER BY c.updatedAt DESC LIMIT 1")
    Optional<SystemEmailConfig> findFirst();
}
