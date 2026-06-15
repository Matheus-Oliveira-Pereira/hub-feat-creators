package com.hubfeatcreators.domain.notificacao.events;

import java.util.UUID;

public record EmailAuthFalhouEvent(UUID accountId, String fromAddress) {}
