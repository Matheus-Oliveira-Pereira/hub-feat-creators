package com.hubfeatcreators.domain.compliance;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "data_processing_records")
public class DataProcessingRecord {

    @Id private UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String finalidade;

    @Column(name = "base_legal", nullable = false)
    @Enumerated(EnumType.STRING)
    private BaseLegal baseLegal;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "dados_coletados", columnDefinition = "text[]")
    private String[] dadosColetados = {};

    @Column(name = "retencao_meses", nullable = false)
    private int retencaoMeses;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "compartilhado_com", columnDefinition = "text[]")
    private String[] compartilhadoCom = {};

    @Column(nullable = false)
    private boolean vigente = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public DataProcessingRecord() {}

    public UUID getId() { return id; }
    public String getFinalidade() { return finalidade; }
    public BaseLegal getBaseLegal() { return baseLegal; }
    public String[] getDadosColetados() { return dadosColetados; }
    public int getRetencaoMeses() { return retencaoMeses; }
    public String[] getCompartilhadoCom() { return compartilhadoCom; }
    public boolean isVigente() { return vigente; }
    public Instant getCreatedAt() { return createdAt; }
}
