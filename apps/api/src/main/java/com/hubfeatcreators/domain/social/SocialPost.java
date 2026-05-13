package com.hubfeatcreators.domain.social;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "social_posts")
public class SocialPost {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "social_account_id", nullable = false)
    private UUID socialAccountId;

    @Column(name = "external_post_id", nullable = false)
    private String externalPostId;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column private String tipo;
    @Column private String caption;

    @Column(nullable = false)
    private int likes = 0;

    @Column(nullable = false)
    private int comments = 0;

    @Column(nullable = false)
    private int shares = 0;

    @Column(nullable = false)
    private int views = 0;

    @Column private String url;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_full", columnDefinition = "jsonb")
    private Map<String, Object> payloadFull;

    protected SocialPost() {}

    public SocialPost(
            UUID socialAccountId,
            String externalPostId,
            Instant postedAt,
            String tipo,
            String caption,
            int likes,
            int comments,
            int shares,
            int views,
            String url,
            Map<String, Object> payloadFull) {
        this.socialAccountId = socialAccountId;
        this.externalPostId = externalPostId;
        this.postedAt = postedAt;
        this.tipo = tipo;
        this.caption = caption;
        this.likes = likes;
        this.comments = comments;
        this.shares = shares;
        this.views = views;
        this.url = url;
        this.payloadFull = payloadFull;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSocialAccountId() {
        return socialAccountId;
    }

    public String getExternalPostId() {
        return externalPostId;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public String getTipo() {
        return tipo;
    }

    public String getCaption() {
        return caption;
    }

    public int getLikes() {
        return likes;
    }

    public int getComments() {
        return comments;
    }

    public int getShares() {
        return shares;
    }

    public int getViews() {
        return views;
    }

    public String getUrl() {
        return url;
    }

    public Map<String, Object> getPayloadFull() {
        return payloadFull;
    }

    public void update(int likes, int comments, int shares, int views) {
        this.likes = likes;
        this.comments = comments;
        this.shares = shares;
        this.views = views;
    }
}
