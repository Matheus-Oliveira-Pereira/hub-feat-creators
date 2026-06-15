package com.hubfeatcreators.portal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hubfeatcreators.domain.portal.*;
import com.hubfeatcreators.domain.tarefa.*;
import com.hubfeatcreators.infra.storage.AttachmentStorage;
import com.hubfeatcreators.infra.web.BusinessException;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class PortalServiceTest {

    @Mock TarefaRepository tarefaRepo;
    @Mock TarefaComentarioRepository comentarioRepo;
    @Mock CreatorEntregavelRepository entregavelRepo;
    @Mock AttachmentStorage storage;

    PortalService service;

    UUID influenciadorId = UUID.randomUUID();
    UUID tarefaId = UUID.randomUUID();
    UUID creatorUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new PortalService(tarefaRepo, comentarioRepo, entregavelRepo, storage);
    }

    private Tarefa makeTarefa(boolean visivel) {
        Tarefa t =
                new Tarefa(
                        "Test task", Instant.now().plusSeconds(3600), creatorUserId, creatorUserId);
        t.setEntidadeTipo(EntidadeTipo.INFLUENCIADOR);
        t.setEntidadeId(influenciadorId);
        t.setVisivelParaCreator(visivel);
        return t;
    }

    // ─── Listar tarefas ───────────────────────────────────────────────────────

    @Test
    void listarTarefas_filtraVisivelParaCreator() {
        Tarefa visible = makeTarefa(true);
        Tarefa hidden = makeTarefa(false);
        when(tarefaRepo.findByEntidade(EntidadeTipo.INFLUENCIADOR, influenciadorId))
                .thenReturn(List.of(visible, hidden));

        var result = service.listarTarefas(influenciadorId);
        assertThat(result).containsExactly(visible);
    }

    // ─── Detalhar tarefa ──────────────────────────────────────────────────────

    @Test
    void detalharTarefa_success() {
        Tarefa t = makeTarefa(true);
        when(tarefaRepo.findById(tarefaId)).thenReturn(Optional.of(t));

        Tarefa result = service.detalharTarefa(influenciadorId, tarefaId);
        assertThat(result).isEqualTo(t);
    }

    @Test
    void detalharTarefa_notVisivel_throws404() {
        Tarefa t = makeTarefa(false);
        when(tarefaRepo.findById(tarefaId)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.detalharTarefa(influenciadorId, tarefaId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void detalharTarefa_wrongInfluenciador_throws404() {
        Tarefa t = makeTarefa(true);
        when(tarefaRepo.findById(tarefaId)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.detalharTarefa(UUID.randomUUID(), tarefaId))
                .isInstanceOf(BusinessException.class);
    }

    // ─── Comentários ──────────────────────────────────────────────────────────

    @Test
    void comentar_savesCreatorComment() {
        Tarefa t = makeTarefa(true);
        when(tarefaRepo.findById(tarefaId)).thenReturn(Optional.of(t));
        when(comentarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var c = service.comentar(influenciadorId, tarefaId, creatorUserId, "Olá!");
        assertThat(c.getAutorTipo()).isEqualTo("CREATOR");
        assertThat(c.isInterno()).isFalse();
        assertThat(c.getTexto()).isEqualTo("Olá!");
    }

    @Test
    void listarComentarios_filtraInternos() {
        Tarefa t = makeTarefa(true);
        when(tarefaRepo.findById(tarefaId)).thenReturn(Optional.of(t));
        TarefaComentario externo = TarefaComentario.fromCreator(tarefaId, creatorUserId, "oi");
        TarefaComentario interno = new TarefaComentario(tarefaId, creatorUserId, "interno");
        when(comentarioRepo.findByTarefaIdOrderByCreatedAtAsc(tarefaId))
                .thenReturn(List.of(externo, interno));

        var result = service.listarComentarios(influenciadorId, tarefaId);
        assertThat(result).containsExactly(externo);
    }

    // ─── Entregáveis ──────────────────────────────────────────────────────────

    @Test
    void enviarEntregavel_storesAndSaves() throws Exception {
        Tarefa t = makeTarefa(true);
        when(tarefaRepo.findById(tarefaId)).thenReturn(Optional.of(t));
        when(storage.store(any(), any(), any(), any(), anyLong()))
                .thenReturn(new AttachmentStorage.StoredFile("path/to/file.pdf", 1024L));
        when(entregavelRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile file =
                new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[1024]);

        var e = service.enviarEntregavel(influenciadorId, tarefaId, creatorUserId, file);
        assertThat(e.getArquivoPath()).isEqualTo("path/to/file.pdf");
        assertThat(e.getStatus()).isEqualTo("ENVIADO");
    }

    @Test
    void revisarEntregavel_updatesStatus() {
        CreatorEntregavel entregavel =
                new CreatorEntregavel(
                        tarefaId, creatorUserId, "path", "doc.pdf", "application/pdf", 1024L);
        when(entregavelRepo.findById(entregavel.getId())).thenReturn(Optional.of(entregavel));
        when(entregavelRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.revisarEntregavel(entregavel.getId(), "APROVADO", "Ótimo!");
        assertThat(result.getStatus()).isEqualTo("APROVADO");
        assertThat(result.getFeedback()).isEqualTo("Ótimo!");
    }

    @Test
    void downloadEntregavel_returnsStream() {
        CreatorEntregavel entregavel =
                new CreatorEntregavel(
                        tarefaId,
                        creatorUserId,
                        "path/doc.pdf",
                        "doc.pdf",
                        "application/pdf",
                        512L);
        when(entregavelRepo.findById(entregavel.getId())).thenReturn(Optional.of(entregavel));
        var fakeStream = new ByteArrayInputStream(new byte[0]);
        when(storage.load("path/doc.pdf")).thenReturn(fakeStream);

        var dl = service.downloadEntregavel(entregavel.getId());
        assertThat(dl.filename()).isEqualTo("doc.pdf");
        assertThat(dl.sizeBytes()).isEqualTo(512L);
    }
}
