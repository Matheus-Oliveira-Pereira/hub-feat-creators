package com.hubfeatcreators.domain.tarefa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "tarefas")
@SQLRestriction("deleted_at IS NULL")
public class Tarefa {

  @Id private UUID id = UUID.randomUUID();

  @Column(name = "assessoria_id", nullable = false)
  private UUID assessoriaId;

  @Column(nullable = false)
  private String titulo;

  @Column
  private String descricao;

  @Column(nullable = false)
  private Instant prazo;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TarefaPrioridade prioridade = TarefaPrioridade.MEDIA;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TarefaStatus status = TarefaStatus.TODO;

  @Column(name = "responsavel_id", nullable = false)
  private UUID responsavelId;

  @Column(name = "criador_id", nullable = false)
  private UUID criadorId;

  @Enumerated(EnumType.STRING)
  @Column(name = "entidade_tipo")
  private EntidadeTipo entidadeTipo;

  @Column(name = "entidade_id")
  private UUID entidadeId;

  @Column(name = "concluida_em")
  private Instant concluidaEm;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  @Column(name = "deleted_at")
  private Instant deletedAt;

  protected Tarefa() {}

  public Tarefa(UUID assessoriaId, String titulo, Instant prazo, UUID responsavelId, UUID criadorId) {
    this.assessoriaId = assessoriaId;
    this.titulo = titulo;
    this.prazo = prazo;
    this.responsavelId = responsavelId;
    this.criadorId = criadorId;
  }

  public UUID getId() { return id; }
  public UUID getAssessoriaId() { return assessoriaId; }
  public String getTitulo() { return titulo; }
  public void setTitulo(String titulo) { this.titulo = titulo; }
  public String getDescricao() { return descricao; }
  public void setDescricao(String descricao) { this.descricao = descricao; }
  public Instant getPrazo() { return prazo; }
  public void setPrazo(Instant prazo) { this.prazo = prazo; }
  public TarefaPrioridade getPrioridade() { return prioridade; }
  public void setPrioridade(TarefaPrioridade prioridade) { this.prioridade = prioridade; }
  public TarefaStatus getStatus() { return status; }
  public void setStatus(TarefaStatus status) { this.status = status; }
  public UUID getResponsavelId() { return responsavelId; }
  public void setResponsavelId(UUID responsavelId) { this.responsavelId = responsavelId; }
  public UUID getCriadorId() { return criadorId; }
  public EntidadeTipo getEntidadeTipo() { return entidadeTipo; }
  public void setEntidadeTipo(EntidadeTipo entidadeTipo) { this.entidadeTipo = entidadeTipo; }
  public UUID getEntidadeId() { return entidadeId; }
  public void setEntidadeId(UUID entidadeId) { this.entidadeId = entidadeId; }
  public Instant getConcluidaEm() { return concluidaEm; }
  public void setConcluidaEm(Instant concluidaEm) { this.concluidaEm = concluidaEm; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
  public Instant getDeletedAt() { return deletedAt; }
  public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }
}
