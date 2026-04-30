package com.hubfeatcreators.infra.security;

import java.util.UUID;

public record AuthPrincipal(UUID usuarioId, UUID assessoriaId, String role) {}
