package com.hubfeatcreators.domain.onboarding;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EmailVerifyTokenRepository extends JpaRepository<EmailVerifyToken, UUID> {
    Optional<EmailVerifyToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM EmailVerifyToken t WHERE t.usuarioId = :usuarioId AND t.usedAt IS NULL")
    void invalidateExisting(UUID usuarioId);
}
