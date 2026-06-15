package com.hubfeatcreators.domain.email;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "system_email_config")
public class SystemEmailConfig {

    @Id private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private int port = 587;

    @Column(nullable = false)
    private String username;

    @Column(name = "password_enc", nullable = false)
    private byte[] passwordEnc;

    @Column(name = "password_nonce", nullable = false)
    private byte[] passwordNonce;

    @Column(name = "from_address", nullable = false)
    private String fromAddress;

    @Column(name = "from_name", nullable = false)
    private String fromName = "feat. creators";

    @Column(name = "tls_mode", nullable = false)
    private String tlsMode = "STARTTLS";

    @Column(name = "daily_quota", nullable = false)
    private int dailyQuota = 500;

    @Column(nullable = false)
    private String status = "ATIVA";

    @Column(name = "falhas_auth_count", nullable = false)
    private int falhasAuthCount = 0;

    @Column(name = "ultima_falha_em")
    private Instant ultimaFalhaEm;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected SystemEmailConfig() {}

    public UUID getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public byte[] getPasswordEnc() {
        return passwordEnc;
    }

    public void setPasswordEnc(byte[] passwordEnc) {
        this.passwordEnc = passwordEnc;
    }

    public byte[] getPasswordNonce() {
        return passwordNonce;
    }

    public void setPasswordNonce(byte[] passwordNonce) {
        this.passwordNonce = passwordNonce;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getTlsMode() {
        return tlsMode;
    }

    public void setTlsMode(String tlsMode) {
        this.tlsMode = tlsMode;
    }

    public int getDailyQuota() {
        return dailyQuota;
    }

    public void setDailyQuota(int dailyQuota) {
        this.dailyQuota = dailyQuota;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getFalhasAuthCount() {
        return falhasAuthCount;
    }

    public void setFalhasAuthCount(int falhasAuthCount) {
        this.falhasAuthCount = falhasAuthCount;
    }

    public Instant getUltimaFalhaEm() {
        return ultimaFalhaEm;
    }

    public void setUltimaFalhaEm(Instant ultimaFalhaEm) {
        this.ultimaFalhaEm = ultimaFalhaEm;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
