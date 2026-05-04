package com.hubfeatcreators.domain.prospeccao;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "prospeccoes")
@Filter(name = "tenant_filter", condition = "assessoria_id = :assessoriaId")
@SQLRestriction("deleted_at IS NULL")
public class Prospeccao {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(name = "marca_id", nullable = false)
    private UUID marcaId;

    @Column(name = "influenciador_id")
    private UUID influenciadorId;

    @Column(name = "assessor_responsavel_id", nullable = false)
    private UUID assessorResponsavelId;

    @Column(nullable = false)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProspeccaoStatus status = ProspeccaoStatus.NOVA;

    @Column(name = "valor_estimado_centavos")
    private Long valorEstimadoCentavos;

    @Column(name = "proxima_acao")
    private String proximaAcao;

    @Column(name = "proxima_acao_em")
    private LocalDate proximaAcaoEm;

    @Column private String observacoes;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false, columnDefinition = "text[]")
    private String[] tags = new String[0];

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_perda")
    private MotivoPerda motivoPerda;

    @Column(name = "motivo_perda_detalhe")
    private String motivoPerdaDetalhe;

    @Column(name = "fechada_em")
    private Instant fechadaEm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    protected Prospeccao() {}

    public Prospeccao(
            UUID assessoriaId,
            UUID marcaId,
            UUID assessorResponsavelId,
            String titulo,
            UUID createdBy) {
        this.assessoriaId = assessoriaId;
        this.marcaId = marcaId;
        this.assessorResponsavelId = assessorResponsavelId;
        this.titulo = titulo;
        this.createdBy = createdBy;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAssessoriaId() {
        return assessoriaId;
    }

    public UUID getMarcaId() {
        return marcaId;
    }

    public void setMarcaId(UUID marcaId) {
        this.marcaId = marcaId;
    }

    public UUID getInfluenciadorId() {
        return influenciadorId;
    }

    public void setInfluenciadorId(UUID influenciadorId) {
        this.influenciadorId = influenciadorId;
    }

    public UUID getAssessorResponsavelId() {
        return assessorResponsavelId;
    }

    public void setAssessorResponsavelId(UUID id) {
        this.assessorResponsavelId = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public ProspeccaoStatus getStatus() {
        return status;
    }

    public void setStatus(ProspeccaoStatus status) {
        this.status = status;
    }

    public Long getValorEstimadoCentavos() {
        return valorEstimadoCentavos;
    }

    public void setValorEstimadoCentavos(Long v) {
        this.valorEstimadoCentavos = v;
    }

    public String getProximaAcao() {
        return proximaAcao;
    }

    public void setProximaAcao(String proximaAcao) {
        this.proximaAcao = proximaAcao;
    }

    public LocalDate getProximaAcaoEm() {
        return proximaAcaoEm;
    }

    public void setProximaAcaoEm(LocalDate proximaAcaoEm) {
        this.proximaAcaoEm = proximaAcaoEm;
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

    public MotivoPerda getMotivoPerda() {
        return motivoPerda;
    }

    public void setMotivoPerda(MotivoPerda motivoPerda) {
        this.motivoPerda = motivoPerda;
    }

    public String getMotivoPerdaDetalhe() {
        return motivoPerdaDetalhe;
    }

    public void setMotivoPerdaDetalhe(String motivoPerdaDetalhe) {
        this.motivoPerdaDetalhe = motivoPerdaDetalhe;
    }

    public Instant getFechadaEm() {
        return fechadaEm;
    }

    public void setFechadaEm(Instant fechadaEm) {
        this.fechadaEm = fechadaEm;
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
}
