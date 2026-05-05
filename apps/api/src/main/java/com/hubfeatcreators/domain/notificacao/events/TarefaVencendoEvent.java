package com.hubfeatcreators.domain.notificacao.events;

import java.util.UUID;

public record TarefaVencendoEvent(UUID assessoriaId, UUID usuarioId, UUID tarefaId, String tarefaTitulo) {}
