package com.hubfeatcreators.domain.compliance;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dsr_solicitacoes")
public class DsrSolicitacao {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(name = "titular_tipo", nullable = false)
    private String titularTipo;

    @Column(name = "titular_id", nullable = false)
    private UUID titularId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TipoDsr tipo;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusDsr status = StatusDsr.PENDENTE;

    @Column(name = "resultado_path")
    private String resultadoPath;

    @Column(name = "prazo_legal_em", nullable = false)
    private Instant prazoLegalEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "atendido_em")
    private Instant atendidoEm;

    public enum TipoDsr {
        ACESSO, CORRECAO, EXCLUSAO, PORTABILIDADE, OPOSICAO
    }

    public enum StatusDsr {
        PENDENTE, EM_ANDAMENTO, CONCLUIDA, REJEITADA
    }

    public DsrSolicitacao() {}

    public DsrSolicitacao(UUID assessoriaId, String titularTipo, UUID titularId, TipoDsr tipo) {
        this.assessoriaId = assessoriaId;
        this.titularTipo = titularTipo;
        this.titularId = titularId;
        this.tipo = tipo;
        // SLA legal LGPD Art. 19 §3: 15 dias
        this.prazoLegalEm = Instant.now().plus(java.time.Duration.ofDays(15));
    }

    public UUID getId() { return id; }
    public UUID getAssessoriaId() { return assessoriaId; }
    public String getTitularTipo() { return titularTipo; }
    public UUID getTitularId() { return titularId; }
    public TipoDsr getTipo() { return tipo; }
    public StatusDsr getStatus() { return status; }
    public void setStatus(StatusDsr status) { this.status = status; }
    public String getResultadoPath() { return resultadoPath; }
    public void setResultadoPath(String resultadoPath) { this.resultadoPath = resultadoPath; }
    public Instant getPrazoLegalEm() { return prazoLegalEm; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtendidoEm() { return atendidoEm; }
    public void setAtendidoEm(Instant atendidoEm) { this.atendidoEm = atendidoEm; }
}
