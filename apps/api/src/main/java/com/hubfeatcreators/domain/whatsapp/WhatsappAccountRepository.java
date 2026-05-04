package com.hubfeatcreators.domain.whatsapp;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhatsappAccountRepository extends JpaRepository<WhatsappAccount, UUID> {
    List<WhatsappAccount> findByAssessoriaIdAndDeletedAtIsNull(UUID assessoriaId);
    Optional<WhatsappAccount> findByIdAndAssessoriaIdAndDeletedAtIsNull(UUID id, UUID assessoriaId);
}
