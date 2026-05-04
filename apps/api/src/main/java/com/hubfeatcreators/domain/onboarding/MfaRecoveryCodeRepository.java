package com.hubfeatcreators.domain.onboarding;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MfaRecoveryCodeRepository extends JpaRepository<MfaRecoveryCode, UUID> {
    List<MfaRecoveryCode> findByUsuarioIdAndUsedAtIsNull(UUID usuarioId);
    Optional<MfaRecoveryCode> findByCodeHashAndUsedAtIsNull(String codeHash);

    @Modifying
    @Query("DELETE FROM MfaRecoveryCode c WHERE c.usuarioId = :usuarioId")
    void deleteAllByUsuarioId(UUID usuarioId);
}
