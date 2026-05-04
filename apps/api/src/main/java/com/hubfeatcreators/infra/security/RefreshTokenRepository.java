package com.hubfeatcreators.infra.security;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByFamilyIdAndRevokedAtIsNull(UUID familyId);

    @Modifying
    @Query(
            "UPDATE RefreshToken rt SET rt.revokedAt = CURRENT_TIMESTAMP WHERE rt.familyId = :familyId AND rt.revokedAt IS NULL")
    void revokeFamily(UUID familyId);

    @Modifying
    @Query(
            "UPDATE RefreshToken rt SET rt.revokedAt = CURRENT_TIMESTAMP WHERE rt.usuarioId = :usuarioId AND rt.revokedAt IS NULL")
    void revokeAllForUser(UUID usuarioId);
}
