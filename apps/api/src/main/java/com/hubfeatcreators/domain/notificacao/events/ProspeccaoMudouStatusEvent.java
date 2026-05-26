package com.hubfeatcreators.domain.notificacao.events;

import java.util.UUID;

public record ProspeccaoMudouStatusEvent(
        UUID responsavelId,
        UUID prospeccaoId,
        String prospeccaoTitulo,
        String statusAnterior,
        String statusNovo) {}
