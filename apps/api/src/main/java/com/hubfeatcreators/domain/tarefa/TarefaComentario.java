package com.hubfeatcreators.domain.tarefa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tarefa_comentarios")
public class TarefaComentario {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "tarefa_id", nullable = false)
    private UUID tarefaId;

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Column(nullable = false)
    private String texto;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected TarefaComentario() {}

    public TarefaComentario(UUID tarefaId, UUID assessoriaId, UUID autorId, String texto) {
        this.tarefaId = tarefaId;
        this.assessoriaId = assessoriaId;
        this.autorId = autorId;
        this.texto = texto;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTarefaId() {
        return tarefaId;
    }

    public UUID getAssessoriaId() {
        return assessoriaId;
    }

    public UUID getAutorId() {
        return autorId;
    }

    public String getTexto() {
        return texto;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
