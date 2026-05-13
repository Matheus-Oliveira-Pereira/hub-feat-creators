package com.hubfeatcreators.domain.portal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "creator_invites")
public class CreatorInvite {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "influenciador_id", nullable = false)
    private UUID influenciadorId;

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(nullable = false)
    private String email;

    @Column(name = "token_hash", nullable = false, unique = true)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "aceito_em")
    private Instant aceitoEm;

    @Column(name = "criado_por_id", nullable = false)
    private UUID criadoPorId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected CreatorInvite() {}

    public CreatorInvite(
            UUID influenciadorId,
            UUID assessoriaId,
            String email,
            String tokenHash,
            Instant expiresAt,
            UUID criadoPorId) {
        this.influenciadorId = influenciadorId;
        this.assessoriaId = assessoriaId;
        this.email = email;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.criadoPorId = criadoPorId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getInfluenciadorId() {
        return influenciadorId;
    }

    public UUID getAssessoriaId() {
        return assessoriaId;
    }

    public String getEmail() {
        return email;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getAceitoEm() {
        return aceitoEm;
    }

    public void setAceitoEm(Instant aceitoEm) {
        this.aceitoEm = aceitoEm;
    }

    public UUID getCriadoPorId() {
        return criadoPorId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isValido() {
        return aceitoEm == null && Instant.now().isBefore(expiresAt);
    }
}
