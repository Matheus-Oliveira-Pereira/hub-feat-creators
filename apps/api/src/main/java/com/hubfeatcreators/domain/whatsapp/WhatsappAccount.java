package com.hubfeatcreators.domain.whatsapp;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_accounts")
public class WhatsappAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(name = "waba_id", nullable = false)
    private String wabaId;

    @Column(name = "phone_number_id", nullable = false)
    private String phoneNumberId;

    @Column(name = "phone_e164", nullable = false)
    private String phoneE164;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "access_token_enc", nullable = false)
    private byte[] accessTokenEnc;

    @Column(name = "token_nonce", nullable = false)
    private byte[] tokenNonce;

    @Column(name = "app_secret_enc", nullable = false)
    private byte[] appSecretEnc;

    @Column(name = "app_secret_nonce", nullable = false)
    private byte[] appSecretNonce;

    @Column(nullable = false)
    private String status = "ATIVO";

    @Column(name = "daily_limit", nullable = false)
    private int dailyLimit = 1000;

    @Column(name = "daily_sent", nullable = false)
    private int dailySent = 0;

    @Column(name = "daily_reset_at")
    private Instant dailyResetAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected WhatsappAccount() {}

    public WhatsappAccount(UUID assessoriaId, String wabaId, String phoneNumberId, String phoneE164,
            String displayName, byte[] accessTokenEnc, byte[] tokenNonce,
            byte[] appSecretEnc, byte[] appSecretNonce) {
        this.assessoriaId = assessoriaId;
        this.wabaId = wabaId;
        this.phoneNumberId = phoneNumberId;
        this.phoneE164 = phoneE164;
        this.displayName = displayName;
        this.accessTokenEnc = accessTokenEnc;
        this.tokenNonce = tokenNonce;
        this.appSecretEnc = appSecretEnc;
        this.appSecretNonce = appSecretNonce;
    }

    public boolean isAtivo() { return "ATIVO".equals(status); }
    public boolean isRateLimited() {
        if (dailyResetAt == null || Instant.now().isAfter(dailyResetAt)) return false;
        return dailySent >= dailyLimit;
    }

    public void incrementSent() {
        Instant now = Instant.now();
        if (dailyResetAt == null || now.isAfter(dailyResetAt)) {
            dailySent = 0;
            dailyResetAt = now.plusSeconds(86400);
        }
        dailySent++;
    }

    public UUID getId() { return id; }
    public UUID getAssessoriaId() { return assessoriaId; }
    public String getWabaId() { return wabaId; }
    public String getPhoneNumberId() { return phoneNumberId; }
    public String getPhoneE164() { return phoneE164; }
    public String getDisplayName() { return displayName; }
    public byte[] getAccessTokenEnc() { return accessTokenEnc; }
    public byte[] getTokenNonce() { return tokenNonce; }
    public byte[] getAppSecretEnc() { return appSecretEnc; }
    public byte[] getAppSecretNonce() { return appSecretNonce; }
    public String getStatus() { return status; }
    public int getDailyLimit() { return dailyLimit; }
    public int getDailySent() { return dailySent; }
    public Instant getDailyResetAt() { return dailyResetAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getDeletedAt() { return deletedAt; }
    public void setStatus(String status) { this.status = status; }
    public void setAccessTokenEnc(byte[] v) { this.accessTokenEnc = v; }
    public void setTokenNonce(byte[] v) { this.tokenNonce = v; }
    public void setAppSecretEnc(byte[] v) { this.appSecretEnc = v; }
    public void setAppSecretNonce(byte[] v) { this.appSecretNonce = v; }
    public void setDisplayName(String v) { this.displayName = v; }
    public void setDeletedAt(Instant v) { this.deletedAt = v; }
}
