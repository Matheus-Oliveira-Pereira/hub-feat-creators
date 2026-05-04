package com.hubfeatcreators.domain.prospeccao;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProspeccaoRepository extends JpaRepository<Prospeccao, UUID> {

  // OWNER vê tudo da assessoria
  @Query(
      "SELECT p FROM Prospeccao p WHERE p.assessoriaId = :assessoriaId "
          + "AND (:status IS NULL OR p.status = :status) "
          + "AND (:assessorId IS NULL OR p.assessorResponsavelId = :assessorId) "
          + "AND (:marcaId IS NULL OR p.marcaId = :marcaId) "
          + "AND (:nome IS NULL OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', :nome, '%')))")
  Page<Prospeccao> findAllOwner(
      @Param("assessoriaId") UUID assessoriaId,
      @Param("status") ProspeccaoStatus status,
      @Param("assessorId") UUID assessorId,
      @Param("marcaId") UUID marcaId,
      @Param("nome") String nome,
      Pageable pageable);

  // ASSESSOR vê: criadas por ele OU onde ele é responsável (PRD-005 visibility)
  @Query(
      "SELECT p FROM Prospeccao p WHERE p.assessoriaId = :assessoriaId "
          + "AND (p.createdBy = :userId OR p.assessorResponsavelId = :userId) "
          + "AND (:status IS NULL OR p.status = :status) "
          + "AND (:assessorId IS NULL OR p.assessorResponsavelId = :assessorId) "
          + "AND (:marcaId IS NULL OR p.marcaId = :marcaId) "
          + "AND (:nome IS NULL OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', :nome, '%')))")
  Page<Prospeccao> findAllAssessor(
      @Param("assessoriaId") UUID assessoriaId,
      @Param("userId") UUID userId,
      @Param("status") ProspeccaoStatus status,
      @Param("assessorId") UUID assessorId,
      @Param("marcaId") UUID marcaId,
      @Param("nome") String nome,
      Pageable pageable);

  // Métricas — counters por status (apenas escopo OWNER, dashboard tenant-wide)
  @Query("SELECT p.status as status, COUNT(p) as total FROM Prospeccao p "
      + "WHERE p.assessoriaId = :assessoriaId GROUP BY p.status")
  List<StatusCount> contarPorStatus(@Param("assessoriaId") UUID assessoriaId);

  @Query("SELECT COUNT(p) FROM Prospeccao p WHERE p.assessoriaId = :assessoriaId "
      + "AND p.fechadaEm >= :desde")
  long countFechadasDesde(
      @Param("assessoriaId") UUID assessoriaId, @Param("desde") java.time.Instant desde);

  /** Média em dias entre createdAt e fechadaEm para FECHADA_GANHA. Postgres-specific. */
  @Query(
      value =
          "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (fechada_em - created_at))/86400.0), 0)"
              + " FROM prospeccoes WHERE assessoria_id = :assessoriaId"
              + " AND status = 'FECHADA_GANHA' AND fechada_em IS NOT NULL"
              + " AND deleted_at IS NULL",
      nativeQuery = true)
  Double timeToCloseDiasMedio(@Param("assessoriaId") UUID assessoriaId);

  interface StatusCount {
    ProspeccaoStatus getStatus();
    long getTotal();
  }
}
