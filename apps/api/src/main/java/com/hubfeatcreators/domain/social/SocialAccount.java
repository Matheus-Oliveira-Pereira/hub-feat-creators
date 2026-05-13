package com.hubfeatcreators.domain.social;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "social_accounts")
public class SocialAccount {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(name = "influenciador_id", nullable = false)
    private UUID influenciadorId;

    @Column(nullable = false)
    private String plataforma;

    @Column(name = "external_user_id", nullable = false)
    private String externalUserId;

    @Column private String handle;

    @Column(name = "access_token_enc", nullable = false)
    private byte[] accessTokenEnc;

    @Column(name = "token_nonce", nullable = false)
    private byte[] tokenNonce;

    @Column(name = "refresh_token_enc")
    private byte[] refreshTokenEnc;

    @Column(name = "refresh_nonce")
    private byte[] refreshNonce;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(nullable = false)
    private String status = "ATIVA";

    @Column(name = "conectado_em", nullable = false, updatable = false)
    private Instant conectadoEm = Instant.now();

    @Column(name = "ultimo_sync_em")
    private Instant ultimoSyncEm;

    protected SocialAccount() {}

    public SocialAccount(
            UUID assessoriaId,
            UUID influenciadorId,
            String plataforma,
            String externalUserId,
            String handle,
            byte[] accessTokenEnc,
            byte[] tokenNonce,
            byte[] refreshTokenEnc,
            byte[] refreshNonce,
            Instant expiresAt) {
        this.assessoriaId = assessoriaId;
        this.influenciadorId = influenciadorId;
        this.plataforma = plataforma;
        this.externalUserId = externalUserId;
        this.handle = handle;
        this.accessTokenEnc = accessTokenEnc;
        this.tokenNonce = tokenNonce;
        this.refreshTokenEnc = refreshTokenEnc;
        this.refreshNonce = refreshNonce;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAssessoriaId() {
        return assessoriaId;
    }

    public UUID getInfluenciadorId() {
        return influenciadorId;
    }

    public String getPlataforma() {
        return plataforma;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public String getHandle() {
        return handle;
    }

    public byte[] getAccessTokenEnc() {
        return accessTokenEnc;
    }

    public byte[] getTokenNonce() {
        return tokenNonce;
    }

    public byte[] getRefreshTokenEnc() {
        return refreshTokenEnc;
    }

    public byte[] getRefreshNonce() {
        return refreshNonce;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public Instant getConectadoEm() {
        return conectadoEm;
    }

    public Instant getUltimoSyncEm() {
        return ultimoSyncEm;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public void setAccessTokenEnc(byte[] accessTokenEnc) {
        this.accessTokenEnc = accessTokenEnc;
    }

    public void setTokenNonce(byte[] tokenNonce) {
        this.tokenNonce = tokenNonce;
    }

    public void setRefreshTokenEnc(byte[] refreshTokenEnc) {
        this.refreshTokenEnc = refreshTokenEnc;
    }

    public void setRefreshNonce(byte[] refreshNonce) {
        this.refreshNonce = refreshNonce;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setUltimoSyncEm(Instant ultimoSyncEm) {
        this.ultimoSyncEm = ultimoSyncEm;
    }

    public void revogar() {
        this.status = "REVOGADA";
        this.accessTokenEnc = null;
        this.tokenNonce = null;
        this.refreshTokenEnc = null;
        this.refreshNonce = null;
    }
}
