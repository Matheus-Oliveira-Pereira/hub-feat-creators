package com.hubfeatcreators.domain.portal;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "creator_entregaveis")
public class CreatorEntregavel {

    @Id private UUID id = UUID.randomUUID();

    @Column(name = "tarefa_id", nullable = false)
    private UUID tarefaId;

    @Column(name = "creator_user_id", nullable = false)
    private UUID creatorUserId;

    @Column(name = "assessoria_id", nullable = false)
    private UUID assessoriaId;

    @Column(name = "arquivo_path", nullable = false)
    private String arquivoPath;

    @Column(nullable = false)
    private String filename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private String status = "ENVIADO";

    @Column
    private String feedback;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm = Instant.now();

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm = Instant.now();

    protected CreatorEntregavel() {}

    public CreatorEntregavel(UUID tarefaId, UUID creatorUserId, UUID assessoriaId,
                              String arquivoPath, String filename, String contentType, long sizeBytes) {
        this.tarefaId = tarefaId;
        this.creatorUserId = creatorUserId;
        this.assessoriaId = assessoriaId;
        this.arquivoPath = arquivoPath;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

    public UUID getId() { return id; }
    public UUID getTarefaId() { return tarefaId; }
    public UUID getCreatorUserId() { return creatorUserId; }
    public UUID getAssessoriaId() { return assessoriaId; }
    public String getArquivoPath() { return arquivoPath; }
    public String getFilename() { return filename; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; this.atualizadoEm = Instant.now(); }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(Instant atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
