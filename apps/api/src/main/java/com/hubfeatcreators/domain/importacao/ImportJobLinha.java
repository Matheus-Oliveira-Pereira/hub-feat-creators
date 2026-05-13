package com.hubfeatcreators.domain.importacao;

import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "import_job_linhas")
public class ImportJobLinha {

    @EmbeddedId private ImportJobLinhaId id;

    @Column(nullable = false)
    private String status;

    @Column(name = "entidade_id")
    private UUID entidadeId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> erros;

    protected ImportJobLinha() {}

    public ImportJobLinha(
            UUID jobId, int linha, String status, UUID entidadeId, List<String> erros) {
        this.id = new ImportJobLinhaId(jobId, linha);
        this.status = status;
        this.entidadeId = entidadeId;
        this.erros = erros != null ? erros : List.of();
    }

    public ImportJobLinhaId getId() {
        return id;
    }

    public UUID getJobId() {
        return id.getJobId();
    }

    public int getLinha() {
        return id.getLinha();
    }

    public String getStatus() {
        return status;
    }

    public UUID getEntidadeId() {
        return entidadeId;
    }

    public List<String> getErros() {
        return erros;
    }
}
