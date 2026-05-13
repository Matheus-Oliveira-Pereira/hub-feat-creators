package com.hubfeatcreators.domain.match;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "creator_match_optout")
public class CreatorMatchOptout {

    @Id
    @Column(name = "influenciador_id")
    private UUID influenciadorId;

    @Column(name = "motivo", columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected CreatorMatchOptout() {}

    public CreatorMatchOptout(UUID influenciadorId, String motivo) {
        this.influenciadorId = influenciadorId;
        this.motivo = motivo;
    }

    public UUID getInfluenciadorId() {
        return influenciadorId;
    }

    public String getMotivo() {
        return motivo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
