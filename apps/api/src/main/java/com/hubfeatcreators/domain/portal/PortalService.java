package com.hubfeatcreators.domain.portal;

import com.hubfeatcreators.domain.tarefa.EntidadeTipo;
import com.hubfeatcreators.domain.tarefa.Tarefa;
import com.hubfeatcreators.domain.tarefa.TarefaComentario;
import com.hubfeatcreators.domain.tarefa.TarefaComentarioRepository;
import com.hubfeatcreators.domain.tarefa.TarefaRepository;
import com.hubfeatcreators.infra.storage.AttachmentStorage;
import com.hubfeatcreators.infra.web.BusinessException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PortalService {

    private final TarefaRepository tarefaRepo;
    private final TarefaComentarioRepository comentarioRepo;
    private final CreatorEntregavelRepository entregavelRepo;
    private final AttachmentStorage storage;

    public PortalService(
            TarefaRepository tarefaRepo,
            TarefaComentarioRepository comentarioRepo,
            CreatorEntregavelRepository entregavelRepo,
            AttachmentStorage storage) {
        this.tarefaRepo = tarefaRepo;
        this.comentarioRepo = comentarioRepo;
        this.entregavelRepo = entregavelRepo;
        this.storage = storage;
    }

    // ─── Tarefas do creator ───────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Tarefa> listarTarefas(UUID assessoriaId, UUID influenciadorId) {
        return tarefaRepo
                .findByEntidade(assessoriaId, EntidadeTipo.INFLUENCIADOR, influenciadorId)
                .stream()
                .filter(Tarefa::isVisivelParaCreator)
                .toList();
    }

    @Transactional(readOnly = true)
    public Tarefa detalharTarefa(UUID assessoriaId, UUID influenciadorId, UUID tarefaId) {
        Tarefa t = tarefaRepo.findById(tarefaId).orElseThrow(BusinessException::notFound);
        if (!t.getAssessoriaId().equals(assessoriaId)) throw BusinessException.notFound();
        if (!t.isVisivelParaCreator()) throw BusinessException.notFound();
        if (!influenciadorId.equals(t.getEntidadeId())) throw BusinessException.notFound();
        return t;
    }

    // ─── Comentários ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TarefaComentario> listarComentarios(
            UUID assessoriaId, UUID influenciadorId, UUID tarefaId) {
        detalharTarefa(assessoriaId, influenciadorId, tarefaId); // validates access
        return comentarioRepo.findByTarefaIdAndAssessoriaId(tarefaId, assessoriaId).stream()
                .filter(c -> !c.isInterno())
                .toList();
    }

    @Transactional
    public TarefaComentario comentar(
            UUID assessoriaId,
            UUID influenciadorId,
            UUID tarefaId,
            UUID creatorUserId,
            String texto) {
        detalharTarefa(assessoriaId, influenciadorId, tarefaId); // validates access
        TarefaComentario c =
                TarefaComentario.fromCreator(tarefaId, assessoriaId, creatorUserId, texto);
        return comentarioRepo.save(c);
    }

    // ─── Entregáveis ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CreatorEntregavel> listarEntregaveis(
            UUID assessoriaId, UUID influenciadorId, UUID tarefaId) {
        detalharTarefa(assessoriaId, influenciadorId, tarefaId);
        return entregavelRepo.findByTarefaIdAndAssessoriaId(tarefaId, assessoriaId);
    }

    @Transactional
    public CreatorEntregavel enviarEntregavel(
            UUID assessoriaId,
            UUID influenciadorId,
            UUID tarefaId,
            UUID creatorUserId,
            MultipartFile file) {
        detalharTarefa(assessoriaId, influenciadorId, tarefaId);

        AttachmentStorage.StoredFile stored;
        try (InputStream is = file.getInputStream()) {
            stored =
                    storage.store(
                            assessoriaId.toString(),
                            file.getOriginalFilename(),
                            file.getContentType(),
                            is,
                            file.getSize());
        } catch (Exception e) {
            throw BusinessException.internalError("STORAGE_ERROR", "Falha ao salvar arquivo.");
        }

        CreatorEntregavel entregavel =
                new CreatorEntregavel(
                        tarefaId,
                        creatorUserId,
                        assessoriaId,
                        stored.storagePath(),
                        file.getOriginalFilename(),
                        file.getContentType(),
                        stored.sizeBytes());
        return entregavelRepo.save(entregavel);
    }

    // ─── Revisão de entregável (assessora) ────────────────────────────────────

    @Transactional
    public CreatorEntregavel revisarEntregavel(
            UUID assessoriaId, UUID entregavelId, String novoStatus, String feedback) {
        CreatorEntregavel e =
                entregavelRepo
                        .findByIdAndAssessoriaId(entregavelId, assessoriaId)
                        .orElseThrow(BusinessException::notFound);
        e.setStatus(novoStatus);
        if (feedback != null) e.setFeedback(feedback);
        return entregavelRepo.save(e);
    }

    // ─── Download entregável ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public DownloadResult downloadEntregavel(UUID assessoriaId, UUID entregavelId) {
        CreatorEntregavel e =
                entregavelRepo
                        .findByIdAndAssessoriaId(entregavelId, assessoriaId)
                        .orElseThrow(BusinessException::notFound);
        InputStream stream = storage.load(e.getArquivoPath());
        return new DownloadResult(stream, e.getFilename(), e.getContentType(), e.getSizeBytes());
    }

    public record DownloadResult(
            InputStream stream, String filename, String contentType, long sizeBytes) {}
}
