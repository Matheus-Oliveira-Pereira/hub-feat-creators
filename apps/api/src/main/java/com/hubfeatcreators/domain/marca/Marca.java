package com.hubfeatcreators.domain.marca;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "marcas")
@Filter(name = "tenant_filter", condition = "assessoria_id = :assessoriaId")
public class Marca {

  @Id private UUID id = UUID.randomUUID();

  @Column(name = "assessoria_id", nullable = false)
  private UUID assessoriaId;

  @Column(nullable = false)
  private String nome;

  @Column private String segmento;

  @Column private String site;

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

  public Marca() {}

  public Marca(UUID assessoriaId, String nome, UUID createdBy) {
    this.assessoriaId = assessoriaId;
    this.nome = nome;
    this.createdBy = createdBy;
  }

  public UUID getId() { return id; }
  public UUID getAssessoriaId() { return assessoriaId; }
  public String getNome() { return nome; }
  public void setNome(String nome) { this.nome = nome; }
  public String getSegmento() { return segmento; }
  public void setSegmento(String segmento) { this.segmento = segmento; }
  public String getSite() { return site; }
  public void setSite(String site) { this.site = site; }
  public String getObservacoes() { return observacoes; }
  public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
  public String[] getTags() { return tags; }
  public void setTags(String[] tags) { this.tags = tags; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
  public Instant getDeletedAt() { return deletedAt; }
  public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
  public UUID getCreatedBy() { return createdBy; }
}
