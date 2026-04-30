package com.hubfeatcreators.infra.tenant;

import java.util.UUID;

public class TenantContext {
  private static final ThreadLocal<UUID> assessoriaId = new ThreadLocal<>();

  public static void setAssessoriaId(UUID id) {
    assessoriaId.set(id);
  }

  public static UUID getAssessoriaId() {
    return assessoriaId.get();
  }

  public static void clear() {
    assessoriaId.remove();
  }
}
