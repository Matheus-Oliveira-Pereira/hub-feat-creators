package com.hubfeatcreators.domain.importacao;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ImportJobLinhaId implements Serializable {

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "linha")
    private int linha;

    protected ImportJobLinhaId() {}

    public ImportJobLinhaId(UUID jobId, int linha) {
        this.jobId = jobId;
        this.linha = linha;
    }

    public UUID getJobId() {
        return jobId;
    }

    public int getLinha() {
        return linha;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImportJobLinhaId that)) return false;
        return linha == that.linha && Objects.equals(jobId, that.jobId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, linha);
    }
}
