package com.hubfeatcreators.domain.match;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "match_feedback")
public class MatchFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sugestao_id", nullable = false)
    private UUID sugestaoId;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Column(name = "sinal", nullable = false)
    private String sinal;

    @Column(name = "comentario", columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected MatchFeedback() {}

    public MatchFeedback(UUID sugestaoId, UUID autorId, String sinal, String comentario) {
        this.sugestaoId = sugestaoId;
        this.autorId = autorId;
        this.sinal = sinal;
        this.comentario = comentario;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSugestaoId() {
        return sugestaoId;
    }

    public UUID getAutorId() {
        return autorId;
    }

    public String getSinal() {
        return sinal;
    }

    public String getComentario() {
        return comentario;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
