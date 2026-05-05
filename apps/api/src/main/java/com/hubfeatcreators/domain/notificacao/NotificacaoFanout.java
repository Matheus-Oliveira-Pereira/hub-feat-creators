package com.hubfeatcreators.domain.notificacao;

import com.hubfeatcreators.domain.notificacao.events.EmailAuthFalhouEvent;
import com.hubfeatcreators.domain.notificacao.events.ProspeccaoMudouStatusEvent;
import com.hubfeatcreators.domain.notificacao.events.TarefaAtrasadaEvent;
import com.hubfeatcreators.domain.notificacao.events.TarefaVencendoEvent;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NotificacaoFanout {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoFanout.class);

    private final NotificacaoService notificacaoService;

    public NotificacaoFanout(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @Async
    @EventListener
    public void onTarefaVencendo(TarefaVencendoEvent e) {
        try {
            notificacaoService.criar(
                    e.assessoriaId(),
                    e.usuarioId(),
                    NotificacaoTipo.TAREFA_VENCENDO,
                    NotificacaoPrioridade.HIGH,
                    "Tarefa vencendo hoje",
                    "\"" + e.tarefaTitulo() + "\" vence hoje.",
                    Map.of("tarefaId", e.tarefaId().toString()),
                    "TAREFA",
                    e.tarefaId());
        } catch (Exception ex) {
            log.error("fanout.tarefa_vencendo.error tarefaId={} msg={}", e.tarefaId(), ex.getMessage(), ex);
        }
    }

    @Async
    @EventListener
    public void onTarefaAtrasada(TarefaAtrasadaEvent e) {
        try {
            notificacaoService.criar(
                    e.assessoriaId(),
                    e.usuarioId(),
                    NotificacaoTipo.TAREFA_ATRASADA,
                    NotificacaoPrioridade.HIGH,
                    "Tarefa atrasada",
                    "\"" + e.tarefaTitulo() + "\" está atrasada.",
                    Map.of("tarefaId", e.tarefaId().toString()),
                    "TAREFA",
                    e.tarefaId());
        } catch (Exception ex) {
            log.error("fanout.tarefa_atrasada.error tarefaId={} msg={}", e.tarefaId(), ex.getMessage(), ex);
        }
    }

    @Async
    @EventListener
    public void onEmailAuthFalhou(EmailAuthFalhouEvent e) {
        try {
            notificacaoService.criar(
                    e.assessoriaId(),
                    null, // broadcast para owners — tratado no service com lookup futuro
                    NotificacaoTipo.EMAIL_AUTH_FALHOU,
                    NotificacaoPrioridade.HIGH,
                    "Falha de autenticação SMTP",
                    "Conta \"" + e.fromAddress() + "\" falhou autenticação SMTP.",
                    Map.of("accountId", e.accountId().toString(), "fromAddress", e.fromAddress()),
                    "EMAIL_ACCOUNT",
                    e.accountId());
        } catch (Exception ex) {
            log.error("fanout.email_auth_falhou.error accountId={} msg={}", e.accountId(), ex.getMessage(), ex);
        }
    }

    @Async
    @EventListener
    public void onProspeccaoMudouStatus(ProspeccaoMudouStatusEvent e) {
        try {
            notificacaoService.criar(
                    e.assessoriaId(),
                    e.responsavelId(),
                    NotificacaoTipo.PROSPECCAO_MUDOU_STATUS,
                    NotificacaoPrioridade.NORMAL,
                    "Prospecção atualizada",
                    "\"" + e.prospeccaoTitulo() + "\" mudou de " + e.statusAnterior() + " para " + e.statusNovo() + ".",
                    Map.of("de", e.statusAnterior(), "para", e.statusNovo()),
                    "PROSPECCAO",
                    e.prospeccaoId());
        } catch (Exception ex) {
            log.error("fanout.prospeccao_status.error prospeccaoId={} msg={}", e.prospeccaoId(), ex.getMessage(), ex);
        }
    }
}
