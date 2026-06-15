package com.hubfeatcreators.domain.convite;

import com.hubfeatcreators.domain.usuario.Usuario;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "convites")
public class Convite {
    @Id private UUID id = UUID.randomUUID();

    @Column(nullable = false, columnDefinition = "CITEXT")
    private String email;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role = Role.ASSESSOR;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "perfil_id")
    private UUID perfilId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private Usuario createdBy;

    public enum Role {
        OWNER,
        ASSESSOR
    }

    public Convite() {}

    public Convite(String email, String token, Role role, Instant expiresAt) {
        this.email = email;
        this.token = token;
        this.role = role;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }

    public Role getRole() {
        return role;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Usuario getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Usuario createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getPerfilId() {
        return perfilId;
    }

    public void setPerfilId(UUID perfilId) {
        this.perfilId = perfilId;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }
}
