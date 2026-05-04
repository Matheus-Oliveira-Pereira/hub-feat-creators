package com.hubfeatcreators.domain.tarefa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario_preferencias")
public class UsuarioPreferencia {

  @Id
  @Column(name = "usuario_id")
  private UUID usuarioId;

  @Column(name = "digest_diario_enabled", nullable = false)
  private boolean digestDiarioEnabled = true;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected UsuarioPreferencia() {}

  public UsuarioPreferencia(UUID usuarioId) {
    this.usuarioId = usuarioId;
  }

  public UUID getUsuarioId() { return usuarioId; }
  public boolean isDigestDiarioEnabled() { return digestDiarioEnabled; }
  public void setDigestDiarioEnabled(boolean enabled) { this.digestDiarioEnabled = enabled; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
