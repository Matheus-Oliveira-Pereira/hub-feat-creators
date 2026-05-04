package com.hubfeatcreators.domain.email;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "email_templates")
@SQLRestriction("deleted_at IS NULL")
public class EmailTemplate {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String assunto;

    @Column(name = "corpo_html", nullable = false)
    private String corpoHtml;

    @Column(name = "corpo_texto")
    private String corpoTexto;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "variaveis_declaradas", nullable = false, columnDefinition = "text[]")
    private String[] variaveisDeclararadas = new String[0];

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected EmailTemplate() {}

    public EmailTemplate(UUID assessoriaId, String nome, String assunto, String corpoHtml) {
        this.assessoriaId = assessoriaId;
        this.nome = nome;
        this.assunto = assunto;
        this.corpoHtml = corpoHtml;
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

    public void setNome(String v) {
        this.nome = v;
    }

    public String getAssunto() {
        return assunto;
    }

    public void setAssunto(String v) {
        this.assunto = v;
    }

    public String getCorpoHtml() {
        return corpoHtml;
    }

    public void setCorpoHtml(String v) {
        this.corpoHtml = v;
    }

    public String getCorpoTexto() {
        return corpoTexto;
    }

    public void setCorpoTexto(String v) {
        this.corpoTexto = v;
    }

    public String[] getVariaveisDeclararadas() {
        return variaveisDeclararadas;
    }

    public void setVariaveisDeclararadas(String[] v) {
        this.variaveisDeclararadas = v;
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
