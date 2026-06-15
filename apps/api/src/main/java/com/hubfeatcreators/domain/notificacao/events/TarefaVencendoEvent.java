package com.hubfeatcreators.domain.notificacao.events;

import java.util.UUID;

public record TarefaVencendoEvent(UUID usuarioId, UUID tarefaId, String tarefaTitulo) {}
