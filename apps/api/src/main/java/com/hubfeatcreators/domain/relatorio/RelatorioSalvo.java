package com.hubfeatcreators.domain.relatorio;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "relatorios_salvos")
public class RelatorioSalvo {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false)
    private String nome;

    @Column(name = "relatorio_tipo", nullable = false)
    private String relatorioTipo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> filtros;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    protected RelatorioSalvo() {}

    public RelatorioSalvo(
            UUID usuarioId,
            String nome,
            String relatorioTipo,
            Map<String, Object> filtros) {
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.relatorioTipo = relatorioTipo;
        this.filtros = filtros;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getRelatorioTipo() {
        return relatorioTipo;
    }

    public Map<String, Object> getFiltros() {
        return filtros;
    }

    public void setFiltros(Map<String, Object> filtros) {
        this.filtros = filtros;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
