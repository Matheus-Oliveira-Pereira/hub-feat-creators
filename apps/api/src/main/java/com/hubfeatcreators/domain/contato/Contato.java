package com.hubfeatcreators.domain.contato;

import com.hubfeatcreators.domain.compliance.BaseLegal;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "contatos")
public class Contato {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "marca_id", nullable = false)
    private UUID marcaId;

    @Column(nullable = false)
    private String nome;

    @Column(columnDefinition = "CITEXT")
    private String email;

    @Column private String telefone;

    @Column private String cargo;

    @Column(name = "email_invalido", nullable = false)
    private boolean emailInvalido = false;

    @Column(name = "base_legal", nullable = false)
    @Enumerated(EnumType.STRING)
    private BaseLegal baseLegal = BaseLegal.LEGITIMO_INTERESSE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Contato() {}

    public Contato(UUID marcaId, String nome) {
        this.marcaId = marcaId;
        this.nome = nome;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMarcaId() {
        return marcaId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public boolean isEmailInvalido() {
        return emailInvalido;
    }

    public void setEmailInvalido(boolean emailInvalido) {
        this.emailInvalido = emailInvalido;
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

    public BaseLegal getBaseLegal() {
        return baseLegal;
    }

    public void setBaseLegal(BaseLegal baseLegal) {
        this.baseLegal = baseLegal;
    }
}
