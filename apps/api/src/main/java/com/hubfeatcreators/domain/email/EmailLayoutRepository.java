package com.hubfeatcreators.domain.email;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EmailLayoutRepository extends JpaRepository<EmailLayout, UUID> {

    @Query("SELECT l FROM EmailLayout l ORDER BY l.id ASC")
    Optional<EmailLayout> findFirst();
}
