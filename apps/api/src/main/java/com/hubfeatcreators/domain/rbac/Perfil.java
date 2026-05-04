package com.hubfeatcreators.domain.rbac;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "perfis",
        uniqueConstraints = @UniqueConstraint(columnNames = {"assessoria_id", "nome"}))
@Filter(name = "tenant_filter", condition = "assessoria_id = :assessoriaId")
@SQLRestriction("deleted_at IS NULL")
public class Perfil {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(nullable = false)
    private String nome;

    @Column private String descricao;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private String[] roles = new String[0];

    @Column(name = "is_system", nullable = false)
    private boolean isSystem = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected Perfil() {}

    public Perfil(
            UUID assessoriaId, String nome, String descricao, Set<String> roles, boolean isSystem) {
        this.assessoriaId = assessoriaId;
        this.nome = nome;
        this.descricao = descricao;
        this.roles = roles.toArray(new String[0]);
        this.isSystem = isSystem;
    }

    public Set<String> rolesAsSet() {
        return new LinkedHashSet<>(java.util.Arrays.asList(roles));
    }

    public UUID getId() {
        return id;
    }

    public UUID getAssessoriaId() {
        return assessoriaId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String[] getRoles() {
        return roles;
    }

    public void setRoles(Set<String> next) {
        this.roles = next.toArray(new String[0]);
    }

    public boolean isSystem() {
        return isSystem;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
