package com.hubfeatcreators.domain.portal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assessoria_branding")
public class AssessoriaBranding {

    @Id
    @Column(name = "assessoria_id")
    private UUID assessoriaId;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "cor_primaria")
    private String corPrimaria;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    protected AssessoriaBranding() {}

    public AssessoriaBranding(UUID assessoriaId) {
        this.assessoriaId = assessoriaId;
    }

    public UUID getAssessoriaId() {
        return assessoriaId;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
        this.atualizadoEm = Instant.now();
    }

    public String getCorPrimaria() {
        return corPrimaria;
    }

    public void setCorPrimaria(String corPrimaria) {
        this.corPrimaria = corPrimaria;
        this.atualizadoEm = Instant.now();
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
