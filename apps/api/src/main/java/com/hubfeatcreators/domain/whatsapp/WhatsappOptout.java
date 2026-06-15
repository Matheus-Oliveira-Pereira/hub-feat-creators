package com.hubfeatcreators.domain.whatsapp;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_optouts")
public class WhatsappOptout {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "citext")
    private String e164;

    private String motivo;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected WhatsappOptout() {}

    public WhatsappOptout(String e164, String motivo) {
        this.e164 = e164;
        this.motivo = motivo;
    }

    public UUID getId() {
        return id;
    }

    public String getE164() {
        return e164;
    }

    public String getMotivo() {
        return motivo;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
