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
@Table(name = "social_consentimentos")
public class SocialConsentimento {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "influenciador_id", nullable = false)
    private UUID influenciadorId;

    @Column(nullable = false)
    private String plataforma;

    @Column(name = "dado_em", nullable = false, updatable = false)
    private Instant dadoEm = Instant.now();

    @Column(name = "revogado_em")
    private Instant revogadoEm;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> prova;

    protected SocialConsentimento() {}

    public SocialConsentimento(UUID influenciadorId, String plataforma, Map<String, Object> prova) {
        this.influenciadorId = influenciadorId;
        this.plataforma = plataforma;
        this.prova = prova;
    }

    public UUID getId() {
        return id;
    }

    public UUID getInfluenciadorId() {
        return influenciadorId;
    }

    public String getPlataforma() {
        return plataforma;
    }

    public Instant getDadoEm() {
        return dadoEm;
    }

    public Instant getRevogadoEm() {
        return revogadoEm;
    }

    public Map<String, Object> getProva() {
        return prova;
    }

    public void revogar() {
        this.revogadoEm = Instant.now();
    }
}
