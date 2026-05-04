package com.hubfeatcreators.domain.email;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_layouts")
public class EmailLayout {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "assessoria_id", nullable = false, unique = true)
    private UUID assessoriaId;

    @Column(name = "header_html", nullable = false)
    private String headerHtml = "";

    @Column(name = "footer_html", nullable = false)
    private String footerHtml =
            "<p style=\"font-size:12px;color:#666\">Para não receber mais e-mails desta assessoria, "
                    + "<a href=\"{{unsubscribe_url}}\">clique aqui</a>.</p>";

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected EmailLayout() {}

    public EmailLayout(UUID assessoriaId) {
        this.assessoriaId = assessoriaId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAssessoriaId() {
        return assessoriaId;
    }

    public String getHeaderHtml() {
        return headerHtml;
    }

    public void setHeaderHtml(String v) {
        this.headerHtml = v;
    }

    public String getFooterHtml() {
        return footerHtml;
    }

    public void setFooterHtml(String v) {
        this.footerHtml = v;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant v) {
        this.updatedAt = v;
    }
}
