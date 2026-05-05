package com.hubfeatcreators.domain.notificacao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notificacoes")
public class Notificacao {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificacaoTipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificacaoPrioridade prioridade = NotificacaoPrioridade.NORMAL;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private String mensagem;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload = Map.of();

    @Column(name = "alvo_tipo")
    private String alvoTipo;

    @Column(name = "alvo_id")
    private UUID alvoId;

    @Column(nullable = false)
    private int agrupadas = 1;

    @Column(name = "lida_em")
    private Instant lidaEm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Notificacao() {}

    public Notificacao(
            UUID assessoriaId,
            UUID usuarioId,
            NotificacaoTipo tipo,
            NotificacaoPrioridade prioridade,
            String titulo,
            String mensagem,
            Map<String, Object> payload,
            String alvoTipo,
            UUID alvoId) {
        this.assessoriaId = assessoriaId;
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.prioridade = prioridade;
        this.titulo = titulo;
        this.mensagem = mensagem;
        this.payload = payload != null ? payload : Map.of();
        this.alvoTipo = alvoTipo;
        this.alvoId = alvoId;
    }

    public UUID getId() { return id; }
    public UUID getAssessoriaId() { return assessoriaId; }
    public UUID getUsuarioId() { return usuarioId; }
    public NotificacaoTipo getTipo() { return tipo; }
    public NotificacaoPrioridade getPrioridade() { return prioridade; }
    public String getTitulo() { return titulo; }
    public String getMensagem() { return mensagem; }
    public Map<String, Object> getPayload() { return payload; }
    public String getAlvoTipo() { return alvoTipo; }
    public UUID getAlvoId() { return alvoId; }
    public int getAgrupadas() { return agrupadas; }
    public Instant getLidaEm() { return lidaEm; }
    public Instant getCreatedAt() { return createdAt; }

    public void setAgrupadas(int agrupadas) { this.agrupadas = agrupadas; }
    public void setLidaEm(Instant lidaEm) { this.lidaEm = lidaEm; }
}
