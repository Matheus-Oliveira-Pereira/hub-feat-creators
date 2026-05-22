package com.hubfeatcreators.infra.security;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import java.util.Set;
import java.util.UUID;

public record AuthPrincipal(
        UUID usuarioId, UUID assessoriaId, String role, Set<String> permissions, String tipo) {

    /** Compat constructor — assume permissions vazio, tipo=INTERNO. */
    public AuthPrincipal(UUID usuarioId, UUID assessoriaId, String role) {
        this(usuarioId, assessoriaId, role, Set.of(), "INTERNO");
    }

    /** Constructor sem tipo (backward compat). */
    public AuthPrincipal(UUID usuarioId, UUID assessoriaId, String role, Set<String> permissions) {
        this(usuarioId, assessoriaId, role, permissions, "INTERNO");
    }

    public boolean isAdm() {
        return "ADM".equals(tipo);
    }

    /** ADMN implícito para tipo=ADM; OWNR bypassa tudo para INTERNO. */
    public boolean hasPermission(String code) {
        if (isAdm()) return true;
        return "OWNER".equals(role)
                || permissions.contains(PermissionCodes.OWNR)
                || permissions.contains(code);
    }

    public boolean hasAnyPermission(Set<String> required) {
        if (isAdm()) return true;
        if ("OWNER".equals(role) || permissions.contains(PermissionCodes.OWNR)) return true;
        for (String r : required) {
            if (permissions.contains(r)) return true;
        }
        return false;
    }

    public boolean hasAllPermissions(Set<String> required) {
        if (isAdm()) return true;
        if ("OWNER".equals(role) || permissions.contains(PermissionCodes.OWNR)) return true;
        return permissions.containsAll(required);
    }
}
