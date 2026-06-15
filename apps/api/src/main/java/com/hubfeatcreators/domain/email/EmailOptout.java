package com.hubfeatcreators.domain.email;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_optouts")
public class EmailOptout {

    @Id private UUID id = UUID.randomUUID();

    @Column(nullable = false, columnDefinition = "citext")
    private String email;

    @Column private String motivo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected EmailOptout() {}

    public EmailOptout(String email, String motivo) {
        this.email = email;
        this.motivo = motivo;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getMotivo() {
        return motivo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
