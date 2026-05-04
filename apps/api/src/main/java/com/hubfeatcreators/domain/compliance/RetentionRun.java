package com.hubfeatcreators.domain.compliance;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "retention_runs")
public class RetentionRun {

    @Id private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private String tabela;

    @Column(nullable = false)
    private int anonimizados = 0;

    @Column(nullable = false)
    private int purgados = 0;

    @Column(name = "duracao_ms", nullable = false)
    private long duracaoMs = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public RetentionRun() {}

    public RetentionRun(LocalDate data, String tabela, int anonimizados, int purgados, long duracaoMs) {
        this.data = data;
        this.tabela = tabela;
        this.anonimizados = anonimizados;
        this.purgados = purgados;
        this.duracaoMs = duracaoMs;
    }

    public UUID getId() { return id; }
    public LocalDate getData() { return data; }
    public String getTabela() { return tabela; }
    public int getAnonimizados() { return anonimizados; }
    public int getPurgados() { return purgados; }
    public long getDuracaoMs() { return duracaoMs; }
    public Instant getCreatedAt() { return createdAt; }
}
