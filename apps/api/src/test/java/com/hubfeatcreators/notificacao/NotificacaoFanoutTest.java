package com.hubfeatcreators.notificacao;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hubfeatcreators.domain.notificacao.NotificacaoCanal;
import com.hubfeatcreators.domain.notificacao.NotificacaoFanout;
import com.hubfeatcreators.domain.notificacao.NotificacaoPrioridade;
import com.hubfeatcreators.domain.notificacao.NotificacaoService;
import com.hubfeatcreators.domain.notificacao.NotificacaoTipo;
import com.hubfeatcreators.domain.notificacao.events.ProspeccaoMudouStatusEvent;
import com.hubfeatcreators.domain.notificacao.events.TarefaVencendoEvent;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificacaoFanoutTest {

    @Mock NotificacaoService notificacaoService;

    @InjectMocks NotificacaoFanout fanout;

    @Test
    void onTarefaVencendo_chama_service_com_tipo_correto() {
        var event =
                new TarefaVencendoEvent(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Tarefa X");

        fanout.onTarefaVencendo(event);

        verify(notificacaoService)
                .criar(
                        eq(event.assessoriaId()),
                        eq(event.usuarioId()),
                        eq(NotificacaoTipo.TAREFA_VENCENDO),
                        eq(NotificacaoPrioridade.HIGH),
                        anyString(),
                        contains("Tarefa X"),
                        anyMap(),
                        eq("TAREFA"),
                        eq(event.tarefaId()));
    }

    @Test
    void onProspeccaoMudouStatus_chama_service_com_tipo_correto() {
        var event =
                new ProspeccaoMudouStatusEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        "Prosp Y",
                        "NOVA",
                        "CONTATADA");

        fanout.onProspeccaoMudouStatus(event);

        verify(notificacaoService)
                .criar(
                        eq(event.assessoriaId()),
                        eq(event.responsavelId()),
                        eq(NotificacaoTipo.PROSPECCAO_MUDOU_STATUS),
                        eq(NotificacaoPrioridade.NORMAL),
                        anyString(),
                        contains("Prosp Y"),
                        anyMap(),
                        eq("PROSPECCAO"),
                        eq(event.prospeccaoId()));
    }

    @Test
    void fanout_nao_propaga_excecao_do_service() {
        var event =
                new TarefaVencendoEvent(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Titulo");
        when(notificacaoService.criar(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("DB down"));

        // deve capturar silenciosamente
        fanout.onTarefaVencendo(event);
    }
}
