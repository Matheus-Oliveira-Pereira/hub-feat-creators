package com.hubfeatcreators.domain.portal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "creator_users")
@SQLRestriction("deleted_at IS NULL")
public class CreatorUser {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "influenciador_id", nullable = false, unique = true)
    private UUID influenciadorId;

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(name = "email_verificado_em")
    private Instant emailVerificadoEm;

    @Column(nullable = false)
    private String status = "ATIVO";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected CreatorUser() {}

    public CreatorUser(UUID influenciadorId, UUID assessoriaId, String email, String senhaHash) {
        this.influenciadorId = influenciadorId;
        this.assessoriaId = assessoriaId;
        this.email = email;
        this.senhaHash = senhaHash;
        this.emailVerificadoEm = Instant.now(); // convite = e-mail verificado
    }

    public UUID getId() { return id; }
    public UUID getInfluenciadorId() { return influenciadorId; }
    public UUID getAssessoriaId() { return assessoriaId; }
    public String getEmail() { return email; }
    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public Instant getEmailVerificadoEm() { return emailVerificadoEm; }
    public void setEmailVerificadoEm(Instant emailVerificadoEm) { this.emailVerificadoEm = emailVerificadoEm; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public boolean isAtivo() { return "ATIVO".equals(status) && deletedAt == null; }
}
