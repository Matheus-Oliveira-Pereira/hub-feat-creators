package com.hubfeatcreators.domain.onboarding;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mfa_recovery_codes")
public class MfaRecoveryCode {
    @Id private UUID id = UUID.randomUUID();

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "code_hash", nullable = false, unique = true)
    private String codeHash;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public MfaRecoveryCode() {}

    public MfaRecoveryCode(UUID usuarioId, String codeHash) {
        this.usuarioId = usuarioId;
        this.codeHash = codeHash;
    }

    public boolean isUsed() { return usedAt != null; }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public String getCodeHash() { return codeHash; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
