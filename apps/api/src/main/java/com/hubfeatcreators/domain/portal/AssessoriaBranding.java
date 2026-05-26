package com.hubfeatcreators.domain.portal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assessoria_branding")
public class AssessoriaBranding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "cor_primaria")
    private String corPrimaria;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    protected AssessoriaBranding() {}

    public AssessoriaBranding(String logoUrl, String corPrimaria) {
        this.logoUrl = logoUrl;
        this.corPrimaria = corPrimaria;
    }

    public UUID getId() {
        return id;
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
