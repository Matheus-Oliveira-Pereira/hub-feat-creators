package com.hubfeatcreators.domain.usuario;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(
        name = "usuarios",
        uniqueConstraints = @UniqueConstraint(columnNames = {"assessoria_id", "email"}))
@Filter(name = "tenant_filter", condition = "assessoria_id = :assessoriaId")
public class Usuario {
    @Id private UUID id = UUID.randomUUID();

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(nullable = false, columnDefinition = "CITEXT")
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role = Role.ASSESSOR;

    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "mfa_secret_enc")
    private String mfaSecretEnc;

    @Column(name = "mfa_ativo", nullable = false)
    private boolean mfaAtivo = false;

    @Column(name = "email_verificado_em")
    private Instant emailVerificadoEm;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.ATIVO;

    @Column(name = "ultimo_login_em")
    private Instant ultimoLoginEm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public enum Role {
        OWNER,
        ASSESSOR
    }

    public enum Status {
        ATIVO,
        INATIVO,
        BLOQUEADO
    }

    public Usuario() {}

    public Usuario(UUID assessoriaId, String email, String senhaHash, Role role) {
        this.assessoriaId = assessoriaId;
        this.email = email;
        this.senhaHash = senhaHash;
        this.role = role;
    }

    public boolean isEmailVerificado() {
        return emailVerificadoEm != null;
    }

    public boolean isAtivo() {
        return status == Status.ATIVO && deletedAt == null;
    }

    public UUID getId() { return id; }
    public UUID getAssessoriaId() { return assessoriaId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public UUID getProfileId() { return profileId; }
    public void setProfileId(UUID profileId) { this.profileId = profileId; }

    public String getMfaSecretEnc() { return mfaSecretEnc; }
    public void setMfaSecretEnc(String mfaSecretEnc) { this.mfaSecretEnc = mfaSecretEnc; }

    public boolean isMfaAtivo() { return mfaAtivo; }
    public void setMfaAtivo(boolean mfaAtivo) { this.mfaAtivo = mfaAtivo; }

    public Instant getEmailVerificadoEm() { return emailVerificadoEm; }
    public void setEmailVerificadoEm(Instant emailVerificadoEm) { this.emailVerificadoEm = emailVerificadoEm; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public Instant getUltimoLoginEm() { return ultimoLoginEm; }
    public void setUltimoLoginEm(Instant ultimoLoginEm) { this.ultimoLoginEm = ultimoLoginEm; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
