package com.hubfeatcreators.domain.admin;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_audit_log")
public class AdminAuditLog {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Column(name = "acao", nullable = false)
    private String acao;

    @Column(name = "assessoria_id_afetada")
    private UUID assessoriaIdAfetada;

    @Column(name = "recurso")
    private String recurso;

    @Column(name = "recurso_id")
    private UUID recursoId;

    @Column(name = "payload_antes", columnDefinition = "jsonb")
    private String payloadAntes;

    @Column(name = "payload_depois", columnDefinition = "jsonb")
    private String payloadDepois;

    @Column(name = "ip")
    private String ip;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public AdminAuditLog() {}

    public AdminAuditLog(
            UUID adminId,
            String acao,
            UUID assessoriaIdAfetada,
            String recurso,
            UUID recursoId,
            String payloadAntes,
            String payloadDepois,
            String ip,
            String userAgent) {
        this.adminId = adminId;
        this.acao = acao;
        this.assessoriaIdAfetada = assessoriaIdAfetada;
        this.recurso = recurso;
        this.recursoId = recursoId;
        this.payloadAntes = payloadAntes;
        this.payloadDepois = payloadDepois;
        this.ip = ip;
        this.userAgent = userAgent;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAdminId() {
        return adminId;
    }

    public String getAcao() {
        return acao;
    }

    public UUID getAssessoriaIdAfetada() {
        return assessoriaIdAfetada;
    }

    public String getRecurso() {
        return recurso;
    }

    public UUID getRecursoId() {
        return recursoId;
    }

    public String getPayloadAntes() {
        return payloadAntes;
    }

    public String getPayloadDepois() {
        return payloadDepois;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
