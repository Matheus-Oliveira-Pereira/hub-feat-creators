package com.hubfeatcreators.infra.security;

import java.util.UUID;

public record CreatorPrincipal(UUID creatorUserId, UUID assessoriaId, UUID influenciadorId) {}
