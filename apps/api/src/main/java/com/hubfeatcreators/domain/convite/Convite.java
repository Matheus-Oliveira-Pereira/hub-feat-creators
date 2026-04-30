package com.hubfeatcreators.domain.convite;

import com.hubfeatcreators.domain.usuario.Usuario;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "convites")
@Filter(name = "tenant_filter", condition = "assessoria_id = :assessoriaId")
public class Convite {
  @Id private UUID id = UUID.randomUUID();

  @Column(name = "assessoria_id", nullable = false)
  private UUID assessoriaId;

  @Column(nullable = false, columnDefinition = "CITEXT")
  private String email;

  @Column(nullable = false, unique = true)
  private String token;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Role role = Role.ASSESSOR;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private Usuario createdBy;

  public enum Role { OWNER, ASSESSOR }

  public Convite() {}

  public Convite(UUID assessoriaId, String email, String token, Role role, Instant expiresAt) {
    this.assessoriaId = assessoriaId;
    this.email = email;
    this.token = token;
    this.role = role;
    this.expiresAt = expiresAt;
  }

  public UUID getId() { return id; }
  public UUID getAssessoriaId() { return assessoriaId; }
  public String getEmail() { return email; }
  public String getToken() { return token; }
  public Role getRole() { return role; }
  public Instant getExpiresAt() { return expiresAt; }
  public Instant getUsedAt() { return usedAt; }
  public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
  public Instant getCreatedAt() { return createdAt; }
  public Usuario getCreatedBy() { return createdBy; }
  public void setCreatedBy(Usuario createdBy) { this.createdBy = createdBy; }

  public boolean isExpired() { return Instant.now().isAfter(expiresAt); }
  public boolean isUsed() { return usedAt != null; }
}
