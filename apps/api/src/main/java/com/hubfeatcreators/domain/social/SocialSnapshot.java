package com.hubfeatcreators.domain.social;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "social_snapshots")
public class SocialSnapshot {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "social_account_id", nullable = false)
    private UUID socialAccountId;

    @Column(nullable = false)
    private LocalDate dia;

    @Column private Integer followers;
    @Column private Integer following;

    @Column(name = "posts_total")
    private Integer postsTotal;

    @Column(name = "engagement_rate", precision = 6, scale = 4)
    private BigDecimal engagementRate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_full", columnDefinition = "jsonb")
    private Map<String, Object> payloadFull;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected SocialSnapshot() {}

    public SocialSnapshot(
            UUID socialAccountId,
            LocalDate dia,
            Integer followers,
            Integer following,
            Integer postsTotal,
            BigDecimal engagementRate,
            Map<String, Object> payloadFull) {
        this.socialAccountId = socialAccountId;
        this.dia = dia;
        this.followers = followers;
        this.following = following;
        this.postsTotal = postsTotal;
        this.engagementRate = engagementRate;
        this.payloadFull = payloadFull;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSocialAccountId() {
        return socialAccountId;
    }

    public LocalDate getDia() {
        return dia;
    }

    public Integer getFollowers() {
        return followers;
    }

    public Integer getFollowing() {
        return following;
    }

    public Integer getPostsTotal() {
        return postsTotal;
    }

    public BigDecimal getEngagementRate() {
        return engagementRate;
    }

    public Map<String, Object> getPayloadFull() {
        return payloadFull;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
