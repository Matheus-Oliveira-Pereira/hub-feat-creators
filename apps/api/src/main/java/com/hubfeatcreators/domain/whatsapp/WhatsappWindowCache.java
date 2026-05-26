package com.hubfeatcreators.domain.whatsapp;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "whatsapp_window_cache")
public class WhatsappWindowCache {

    @Id
    @Column(name = "e164", nullable = false)
    private String e164;

    @Column(name = "last_inbound_at", nullable = false)
    private Instant lastInboundAt;

    protected WhatsappWindowCache() {}

    public WhatsappWindowCache(String e164, Instant lastInboundAt) {
        this.e164 = e164;
        this.lastInboundAt = lastInboundAt;
    }

    public boolean isWindowOpen() {
        return lastInboundAt != null && Instant.now().isBefore(lastInboundAt.plusSeconds(86400));
    }

    public String getId() {
        return e164;
    }

    public String getE164() {
        return e164;
    }

    public Instant getLastInboundAt() {
        return lastInboundAt;
    }

    public void setLastInboundAt(Instant v) {
        this.lastInboundAt = v;
    }
}
