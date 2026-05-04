package com.hubfeatcreators.domain.whatsapp;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_window_cache")
public class WhatsappWindowCache {

    @EmbeddedId
    private WindowKey id;

    @Column(name = "last_inbound_at", nullable = false)
    private Instant lastInboundAt;

    protected WhatsappWindowCache() {}

    public WhatsappWindowCache(UUID assessoriaId, String e164, Instant lastInboundAt) {
        this.id = new WindowKey(assessoriaId, e164);
        this.lastInboundAt = lastInboundAt;
    }

    public boolean isWindowOpen() {
        return lastInboundAt != null && Instant.now().isBefore(lastInboundAt.plusSeconds(86400));
    }

    public WindowKey getId() { return id; }
    public Instant getLastInboundAt() { return lastInboundAt; }
    public void setLastInboundAt(Instant v) { this.lastInboundAt = v; }

    @Embeddable
    public static class WindowKey implements java.io.Serializable {
        @Column(name = "assessoria_id")
        private UUID assessoriaId;
        @Column(name = "e164")
        private String e164;

        protected WindowKey() {}
        public WindowKey(UUID assessoriaId, String e164) {
            this.assessoriaId = assessoriaId;
            this.e164 = e164;
        }
        public UUID getAssessoriaId() { return assessoriaId; }
        public String getE164() { return e164; }
    }
}
