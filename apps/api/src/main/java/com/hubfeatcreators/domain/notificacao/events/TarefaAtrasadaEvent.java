package com.hubfeatcreators.domain.notificacao.events;

import java.util.UUID;

public record TarefaAtrasadaEvent(UUID assessoriaId, UUID usuarioId, UUID tarefaId, String tarefaTitulo) {}
