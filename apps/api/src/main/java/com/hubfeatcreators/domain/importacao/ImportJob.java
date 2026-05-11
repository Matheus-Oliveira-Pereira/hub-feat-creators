package com.hubfeatcreators.domain.importacao;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "import_jobs")
public class ImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(nullable = false)
    private String entidade;

    @Column(name = "arquivo_path", nullable = false)
    private String arquivoPath;

    @Column(name = "arquivo_nome", nullable = false)
    private String arquivoNome;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> mapeamento;

    @Column(name = "base_legal")
    private String baseLegal;

    @Column(name = "dedup_strategy", nullable = false)
    private String dedupStrategy = "SKIP";

    @Column(nullable = false)
    private String status = "UPLOADADO";

    @Column(name = "total_linhas")
    private Integer totalLinhas;

    @Column(nullable = false)
    private int processadas = 0;

    @Column(nullable = false)
    private int sucesso = 0;

    @Column(nullable = false)
    private int falha = 0;

    @Column(name = "cancelable_batch_id")
    private UUID cancelableBatchId;

    @Column(name = "iniciado_em")
    private Instant iniciadoEm;

    @Column(name = "concluido_em")
    private Instant concluidoEm;

    @Column(name = "relatorio_path")
    private String relatorioPath;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    protected ImportJob() {}

    public ImportJob(UUID assessoriaId, UUID usuarioId, String entidade, String arquivoPath, String arquivoNome) {
        this.assessoriaId = assessoriaId;
        this.usuarioId = usuarioId;
        this.entidade = entidade;
        this.arquivoPath = arquivoPath;
        this.arquivoNome = arquivoNome;
    }

    public UUID getId() { return id; }
    public UUID getAssessoriaId() { return assessoriaId; }
    public UUID getUsuarioId() { return usuarioId; }
    public String getEntidade() { return entidade; }
    public String getArquivoPath() { return arquivoPath; }
    public String getArquivoNome() { return arquivoNome; }
    public Map<String, String> getMapeamento() { return mapeamento; }
    public void setMapeamento(Map<String, String> mapeamento) { this.mapeamento = mapeamento; }
    public String getBaseLegal() { return baseLegal; }
    public void setBaseLegal(String baseLegal) { this.baseLegal = baseLegal; }
    public String getDedupStrategy() { return dedupStrategy; }
    public void setDedupStrategy(String dedupStrategy) { this.dedupStrategy = dedupStrategy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTotalLinhas() { return totalLinhas; }
    public void setTotalLinhas(Integer totalLinhas) { this.totalLinhas = totalLinhas; }
    public int getProcessadas() { return processadas; }
    public void setProcessadas(int processadas) { this.processadas = processadas; }
    public int getSucesso() { return sucesso; }
    public void setSucesso(int sucesso) { this.sucesso = sucesso; }
    public int getFalha() { return falha; }
    public void setFalha(int falha) { this.falha = falha; }
    public UUID getCancelableBatchId() { return cancelableBatchId; }
    public void setCancelableBatchId(UUID cancelableBatchId) { this.cancelableBatchId = cancelableBatchId; }
    public Instant getIniciadoEm() { return iniciadoEm; }
    public void setIniciadoEm(Instant iniciadoEm) { this.iniciadoEm = iniciadoEm; }
    public Instant getConcluidoEm() { return concluidoEm; }
    public void setConcluidoEm(Instant concluidoEm) { this.concluidoEm = concluidoEm; }
    public String getRelatorioPath() { return relatorioPath; }
    public void setRelatorioPath(String relatorioPath) { this.relatorioPath = relatorioPath; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
