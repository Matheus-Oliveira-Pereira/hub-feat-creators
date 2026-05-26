package com.hubfeatcreators.domain.influenciador;

import com.hubfeatcreators.domain.compliance.BaseLegal;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "influenciadores")
public class Influenciador {

    @Id private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String nome;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "handles", columnDefinition = "jsonb")
    private Map<String, String> handles = new HashMap<>();

    @Column private String nicho;

    @Column(name = "audiencia_total")
    private Long audienciaTotal;

    @Column(columnDefinition = "text")
    private String observacoes;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "text[]")
    private String[] tags = {};

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "assessor_responsavel_id")
    private UUID assessorResponsavelId;

    @Column(name = "base_legal", nullable = false)
    @Enumerated(EnumType.STRING)
    private BaseLegal baseLegal = BaseLegal.LEGITIMO_INTERESSE;

    public Influenciador() {}

    public Influenciador(String nome, UUID createdBy) {
        this.nome = nome;
        this.createdBy = createdBy;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Map<String, String> getHandles() {
        return handles;
    }

    public void setHandles(Map<String, String> handles) {
        this.handles = handles;
    }

    public String getNicho() {
        return nicho;
    }

    public void setNicho(String nicho) {
        this.nicho = nicho;
    }

    public Long getAudienciaTotal() {
        return audienciaTotal;
    }

    public void setAudienciaTotal(Long audienciaTotal) {
        this.audienciaTotal = audienciaTotal;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
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

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getAssessorResponsavelId() {
        return assessorResponsavelId;
    }

    public void setAssessorResponsavelId(UUID assessorResponsavelId) {
        this.assessorResponsavelId = assessorResponsavelId;
    }

    public BaseLegal getBaseLegal() {
        return baseLegal;
    }

    public void setBaseLegal(BaseLegal baseLegal) {
        this.baseLegal = baseLegal;
    }
}
