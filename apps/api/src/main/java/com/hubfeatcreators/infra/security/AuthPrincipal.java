package com.hubfeatcreators.infra.security;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import java.util.Set;
import java.util.UUID;

public record AuthPrincipal(UUID usuarioId, String role, Set<String> permissions) {

    public AuthPrincipal(UUID usuarioId, String role) {
        this(usuarioId, role, Set.of());
    }

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
