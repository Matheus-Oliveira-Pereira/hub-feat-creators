package com.hubfeatcreators.domain.email;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailEventoRepository extends JpaRepository<EmailEvento, UUID> {
    List<EmailEvento> findByEnvioIdOrderByCreatedAtDesc(UUID envioId);

    Optional<EmailEvento> findFirstByEnvioIdAndTipo(UUID envioId, EmailEventoTipo tipo);
}
