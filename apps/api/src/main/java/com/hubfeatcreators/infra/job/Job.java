package com.hubfeatcreators.infra.job;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "jobs")
public class Job {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "assessoria_id")
    private UUID assessoriaId;

    @Column(nullable = false)
    private String tipo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.PENDENTE;

    @Column(name = "agendado_para", nullable = false)
    private Instant agendadoPara = Instant.now();

    @Column(name = "proxima_tentativa_em", nullable = false)
    private Instant proximaTentativaEm = Instant.now();

    @Column(nullable = false)
    private int tentativas = 0;

    @Column(name = "max_tentativas", nullable = false)
    private int maxTentativas = 4;

    @Column(name = "idempotency_key")
    private UUID idempotencyKey;

    @Column(name = "ultimo_erro")
    private String ultimoErro;

    @Column(name = "iniciado_em")
    private Instant iniciadoEm;

    @Column(name = "concluido_em")
    private Instant concluidoEm;

    @Column(name = "worker_id")
    private String workerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Job() {}

    public Job(UUID assessoriaId, String tipo, Map<String, Object> payload, UUID idempotencyKey) {
        this.assessoriaId = assessoriaId;
        this.tipo = tipo;
        this.payload = payload;
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAssessoriaId() {
        return assessoriaId;
    }

    public String getTipo() {
        return tipo;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Instant getAgendadoPara() {
        return agendadoPara;
    }

    public void setAgendadoPara(Instant agendadoPara) {
        this.agendadoPara = agendadoPara;
    }

    public Instant getProximaTentativaEm() {
        return proximaTentativaEm;
    }

    public void setProximaTentativaEm(Instant proximaTentativaEm) {
        this.proximaTentativaEm = proximaTentativaEm;
    }

    public int getTentativas() {
        return tentativas;
    }

    public void setTentativas(int tentativas) {
        this.tentativas = tentativas;
    }

    public int getMaxTentativas() {
        return maxTentativas;
    }

    public UUID getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getUltimoErro() {
        return ultimoErro;
    }

    public void setUltimoErro(String ultimoErro) {
        this.ultimoErro = ultimoErro;
    }

    public Instant getIniciadoEm() {
        return iniciadoEm;
    }

    public void setIniciadoEm(Instant iniciadoEm) {
        this.iniciadoEm = iniciadoEm;
    }

    public Instant getConcluidoEm() {
        return concluidoEm;
    }

    public void setConcluidoEm(Instant concluidoEm) {
        this.concluidoEm = concluidoEm;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
