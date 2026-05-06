package com.hubfeatcreators.domain.historico;

public enum EventoTipo {
    // Prospecção
    PROSPECCAO_CRIADA,
    PROSPECCAO_STATUS_MUDOU,
    PROSPECCAO_COMENTARIO,
    PROSPECCAO_FECHADA_GANHA,
    PROSPECCAO_FECHADA_PERDIDA,

    // Tarefa
    TAREFA_CRIADA,
    TAREFA_CONCLUIDA,
    TAREFA_REATRIBUIDA,

    // E-mail
    EMAIL_ENVIADO,
    EMAIL_BOUNCED,

    // WhatsApp
    WA_ENVIADO,

    // Cadastro
    INFLUENCIADOR_CRIADO,
    MARCA_CRIADA,
    CONTATO_CRIADO,
}
