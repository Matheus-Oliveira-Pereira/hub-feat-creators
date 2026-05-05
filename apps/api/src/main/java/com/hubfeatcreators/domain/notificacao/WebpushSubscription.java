package com.hubfeatcreators.domain.notificacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webpush_subscriptions")
public class WebpushSubscription {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false, unique = true)
    private String endpoint;

    @Column(nullable = false)
    private String p256dh;

    @Column(name = "auth_secret", nullable = false)
    private String authSecret;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    protected WebpushSubscription() {}

    public WebpushSubscription(UUID usuarioId, String endpoint, String p256dh, String authSecret, String userAgent) {
        this.usuarioId = usuarioId;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.authSecret = authSecret;
        this.userAgent = userAgent;
    }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public String getEndpoint() { return endpoint; }
    public String getP256dh() { return p256dh; }
    public String getAuthSecret() { return authSecret; }
    public String getUserAgent() { return userAgent; }
    public boolean isAtiva() { return ativa; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastUsedAt() { return lastUsedAt; }

    public void setAtiva(boolean ativa) { this.ativa = ativa; }
    public void setLastUsedAt(Instant lastUsedAt) { this.lastUsedAt = lastUsedAt; }
}
