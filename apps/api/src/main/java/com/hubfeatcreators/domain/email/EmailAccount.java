package com.hubfeatcreators.domain.email;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "email_accounts")
@SQLRestriction("deleted_at IS NULL")
public class EmailAccount {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private int port;

    @Column(nullable = false)
    private String username;

    @Column(name = "password_encrypted", nullable = false)
    private byte[] passwordEncrypted;

    @Column(name = "password_nonce", nullable = false)
    private byte[] passwordNonce;

    @Column(name = "from_address", nullable = false)
    private String fromAddress;

    @Column(name = "from_name", nullable = false)
    private String fromName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tls_mode", nullable = false)
    private TlsMode tlsMode = TlsMode.STARTTLS;

    @Column(name = "daily_quota", nullable = false)
    private int dailyQuota = 500;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailAccountStatus status = EmailAccountStatus.ATIVA;

    @Column(name = "falhas_auth_count", nullable = false)
    private int falhasAuthCount = 0;

    @Column(name = "ultima_falha_em")
    private Instant ultimaFalhaEm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected EmailAccount() {}

    public EmailAccount(
            UUID assessoriaId,
            String nome,
            String host,
            int port,
            String username,
            byte[] passwordEncrypted,
            byte[] passwordNonce,
            String fromAddress,
            String fromName,
            TlsMode tlsMode,
            int dailyQuota) {
        this.assessoriaId = assessoriaId;
        this.nome = nome;
        this.host = host;
        this.port = port;
        this.username = username;
        this.passwordEncrypted = passwordEncrypted;
        this.passwordNonce = passwordNonce;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.tlsMode = tlsMode;
        this.dailyQuota = dailyQuota;
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

    public byte[] getPasswordEncrypted() {
        return passwordEncrypted;
    }

    public void setPasswordEncrypted(byte[] v) {
        this.passwordEncrypted = v;
    }

    public byte[] getPasswordNonce() {
        return passwordNonce;
    }

    public void setPasswordNonce(byte[] v) {
        this.passwordNonce = v;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String v) {
        this.fromAddress = v;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String v) {
        this.fromName = v;
    }

    public TlsMode getTlsMode() {
        return tlsMode;
    }

    public void setTlsMode(TlsMode v) {
        this.tlsMode = v;
    }

    public int getDailyQuota() {
        return dailyQuota;
    }

    public void setDailyQuota(int v) {
        this.dailyQuota = v;
    }

    public EmailAccountStatus getStatus() {
        return status;
    }

    public void setStatus(EmailAccountStatus v) {
        this.status = v;
    }

    public int getFalhasAuthCount() {
        return falhasAuthCount;
    }

    public void setFalhasAuthCount(int v) {
        this.falhasAuthCount = v;
    }

    public Instant getUltimaFalhaEm() {
        return ultimaFalhaEm;
    }

    public void setUltimaFalhaEm(Instant v) {
        this.ultimaFalhaEm = v;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant v) {
        this.updatedAt = v;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant v) {
        this.deletedAt = v;
    }
}
