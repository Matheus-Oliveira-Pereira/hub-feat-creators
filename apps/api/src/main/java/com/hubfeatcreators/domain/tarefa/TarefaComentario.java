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

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Column(nullable = false)
    private String texto;

    @Column(nullable = false)
    private boolean interno = true;

    @Column(name = "autor_tipo", nullable = false)
    private String autorTipo = "USUARIO";

    @Column(name = "creator_user_id")
    private UUID creatorUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected TarefaComentario() {}

    public TarefaComentario(UUID tarefaId, UUID autorId, String texto) {
        this.tarefaId = tarefaId;
        this.autorId = autorId;
        this.texto = texto;
    }

    public static TarefaComentario fromCreator(UUID tarefaId, UUID creatorUserId, String texto) {
        TarefaComentario c = new TarefaComentario();
        c.tarefaId = tarefaId;
        c.autorId = creatorUserId;
        c.creatorUserId = creatorUserId;
        c.texto = texto;
        c.interno = false;
        c.autorTipo = "CREATOR";
        return c;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTarefaId() {
        return tarefaId;
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

    public boolean isInterno() {
        return interno;
    }

    public void setInterno(boolean interno) {
        this.interno = interno;
    }

    public String getAutorTipo() {
        return autorTipo;
    }

    public UUID getCreatorUserId() {
        return creatorUserId;
    }
}
