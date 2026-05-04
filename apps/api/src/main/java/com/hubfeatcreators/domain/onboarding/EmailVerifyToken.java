package com.hubfeatcreators.domain.onboarding;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_verify_tokens")
public class EmailVerifyToken {
    @Id private UUID id = UUID.randomUUID();

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public EmailVerifyToken() {}

    public EmailVerifyToken(UUID usuarioId, String tokenHash, Instant expiresAt) {
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    public boolean isUsed() { return usedAt != null; }
    public boolean isValid() { return !isExpired() && !isUsed(); }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
    public Instant getCreatedAt() { return createdAt; }
}
