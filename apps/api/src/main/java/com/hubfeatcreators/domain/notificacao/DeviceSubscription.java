package com.hubfeatcreators.domain.notificacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_subscriptions")
public class DeviceSubscription {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "user_tipo", nullable = false)
    private String userTipo;

    @Column(nullable = false)
    private String canal;

    @Column(nullable = false, unique = true)
    private String token;

    @Column private String plataforma;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "ultimo_uso")
    private Instant ultimoUso;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected DeviceSubscription() {}

    public DeviceSubscription(
            UUID userId, String userTipo, String canal, String token, String plataforma) {
        this.userId = userId;
        this.userTipo = userTipo;
        this.canal = canal;
        this.token = token;
        this.plataforma = plataforma;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUserTipo() {
        return userTipo;
    }

    public String getCanal() {
        return canal;
    }

    public String getToken() {
        return token;
    }

    public String getPlataforma() {
        return plataforma;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public Instant getUltimoUso() {
        return ultimoUso;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setAtiva(boolean ativa) {
        this.ativa = ativa;
    }

    public void setUltimoUso(Instant ultimoUso) {
        this.ultimoUso = ultimoUso;
    }
}
