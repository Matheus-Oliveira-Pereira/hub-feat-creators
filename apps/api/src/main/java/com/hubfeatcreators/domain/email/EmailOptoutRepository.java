package com.hubfeatcreators.domain.email;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailOptoutRepository extends JpaRepository<EmailOptout, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<EmailOptout> findByEmailIgnoreCase(String email);
}
