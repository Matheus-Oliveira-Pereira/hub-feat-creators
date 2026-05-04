package com.hubfeatcreators.infra.security;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import java.util.Set;
import java.util.UUID;

public record AuthPrincipal(
        UUID usuarioId, UUID assessoriaId, String role, Set<String> permissions) {

    /** Compat constructor — assume permissions vazio. */
    public AuthPrincipal(UUID usuarioId, UUID assessoriaId, String role) {
        this(usuarioId, assessoriaId, role, Set.of());
    }

    /** OWNR bypassa qualquer checagem; OWNER coarse também (compat com PRD-001). */
    public boolean hasPermission(String code) {
        return "OWNER".equals(role)
                || permissions.contains(PermissionCodes.OWNR)
                || permissions.contains(code);
    }

    public boolean hasAnyPermission(Set<String> required) {
        if ("OWNER".equals(role) || permissions.contains(PermissionCodes.OWNR)) return true;
        for (String r : required) {
            if (permissions.contains(r)) return true;
        }
        return false;
    }

    public boolean hasAllPermissions(Set<String> required) {
        if ("OWNER".equals(role) || permissions.contains(PermissionCodes.OWNR)) return true;
        return permissions.containsAll(required);
    }
}
