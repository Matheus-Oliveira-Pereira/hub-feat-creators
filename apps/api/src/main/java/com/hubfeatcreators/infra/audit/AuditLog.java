package com.hubfeatcreators.infra.audit;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(nullable = false)
    private String entidade;

    @Column(name = "entidade_id", nullable = false)
    private UUID entidadeId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Acao acao;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "ip")
    private String ip;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public enum Acao {
        CREATE, UPDATE, DELETE, RESTORE,
        LOGIN, LOGIN_FAILED, LOGOUT,
        EMAIL_VERIFIED,
        PASSWORD_RESET_REQUEST, PASSWORD_RESET,
        MFA_ENABLED, MFA_DISABLED, MFA_RECOVERY_USED,
        INVITE_SENT, INVITE_ACCEPTED,
        MEMBER_DEACTIVATED, MEMBER_ACTIVATED, MEMBER_REMOVED,
        SIGNUP, LOCKOUT
    }

    public AuditLog() {}

    public AuditLog(
            UUID assessoriaId,
            UUID usuarioId,
            String entidade,
            UUID entidadeId,
            Acao acao,
            Map<String, Object> payload) {
        this.assessoriaId = assessoriaId;
        this.usuarioId = usuarioId;
        this.entidade = entidade;
        this.entidadeId = entidadeId;
        this.acao = acao;
        this.payload = payload;
    }

    public AuditLog(
            UUID assessoriaId,
            UUID usuarioId,
            String entidade,
            UUID entidadeId,
            Acao acao,
            Map<String, Object> payload,
            String ip,
            String userAgent) {
        this(assessoriaId, usuarioId, entidade, entidadeId, acao, payload);
        this.ip = ip;
        this.userAgent = userAgent;
    }

    public UUID getId() { return id; }
    public UUID getAssessoriaId() { return assessoriaId; }
    public UUID getUsuarioId() { return usuarioId; }
    public String getEntidade() { return entidade; }
    public UUID getEntidadeId() { return entidadeId; }
    public Acao getAcao() { return acao; }
    public Map<String, Object> getPayload() { return payload; }
    public String getIp() { return ip; }
    public String getUserAgent() { return userAgent; }
    public Instant getCreatedAt() { return createdAt; }
}
