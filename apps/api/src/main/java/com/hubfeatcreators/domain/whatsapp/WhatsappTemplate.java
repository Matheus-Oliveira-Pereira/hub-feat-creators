package com.hubfeatcreators.domain.whatsapp;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "whatsapp_templates")
public class WhatsappTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String idioma = "pt_BR";

    @Column(nullable = false)
    private String categoria;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String corpo;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]", nullable = false)
    private String[] variaveis = {};

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "meta_template_id")
    private String metaTemplateId;

    @Column(name = "motivo_rejeicao")
    private String motivoRejeicao;

    @Column(name = "submetido_em")
    private Instant submetidoEm;

    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected WhatsappTemplate() {}

    public WhatsappTemplate(UUID assessoriaId, UUID accountId, String nome, String idioma,
            String categoria, String corpo, String[] variaveis) {
        this.assessoriaId = assessoriaId;
        this.accountId = accountId;
        this.nome = nome;
        this.idioma = idioma;
        this.categoria = categoria;
        this.corpo = corpo;
        this.variaveis = variaveis != null ? variaveis : new String[]{};
    }

    public UUID getId() { return id; }
    public UUID getAssessoriaId() { return assessoriaId; }
    public UUID getAccountId() { return accountId; }
    public String getNome() { return nome; }
    public String getIdioma() { return idioma; }
    public String getCategoria() { return categoria; }
    public String getCorpo() { return corpo; }
    public String[] getVariaveis() { return variaveis; }
    public String getStatus() { return status; }
    public String getMetaTemplateId() { return metaTemplateId; }
    public String getMotivoRejeicao() { return motivoRejeicao; }
    public Instant getSubmetidoEm() { return submetidoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public Instant getCreatedAt() { return createdAt; }
    public void setStatus(String v) { this.status = v; }
    public void setMetaTemplateId(String v) { this.metaTemplateId = v; }
    public void setMotivoRejeicao(String v) { this.motivoRejeicao = v; }
    public void setSubmetidoEm(Instant v) { this.submetidoEm = v; }
    public void setAtualizadoEm(Instant v) { this.atualizadoEm = v; }
    public void setCorpo(String v) { this.corpo = v; }
    public void setVariaveis(String[] v) { this.variaveis = v; }
}
