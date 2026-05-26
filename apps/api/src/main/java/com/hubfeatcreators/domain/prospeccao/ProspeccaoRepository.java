package com.hubfeatcreators.domain.prospeccao;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProspeccaoRepository extends JpaRepository<Prospeccao, UUID> {

    // OWNER vê tudo
    @Query(
            "SELECT p FROM Prospeccao p WHERE"
                    + " (:status IS NULL OR p.status = :status)"
                    + " AND (:assessorId IS NULL OR p.assessorResponsavelId = :assessorId)"
                    + " AND (:marcaId IS NULL OR p.marcaId = :marcaId)"
                    + " AND (:nome IS NULL OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', CAST(:nome AS string), '%')))")
    Page<Prospeccao> findAllOwner(
            @Param("status") ProspeccaoStatus status,
            @Param("assessorId") UUID assessorId,
            @Param("marcaId") UUID marcaId,
            @Param("nome") String nome,
            Pageable pageable);

    // ASSESSOR vê: criadas por ele OU onde ele é responsável (PRD-005 visibility)
    @Query(
            "SELECT p FROM Prospeccao p WHERE"
                    + " (p.createdBy = :userId OR p.assessorResponsavelId = :userId)"
                    + " AND (:status IS NULL OR p.status = :status)"
                    + " AND (:assessorId IS NULL OR p.assessorResponsavelId = :assessorId)"
                    + " AND (:marcaId IS NULL OR p.marcaId = :marcaId)"
                    + " AND (:nome IS NULL OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', CAST(:nome AS string), '%')))")
    Page<Prospeccao> findAllAssessor(
            @Param("userId") UUID userId,
            @Param("status") ProspeccaoStatus status,
            @Param("assessorId") UUID assessorId,
            @Param("marcaId") UUID marcaId,
            @Param("nome") String nome,
            Pageable pageable);

    // Métricas — counters por status
    @Query("SELECT p.status as status, COUNT(p) as total FROM Prospeccao p GROUP BY p.status")
    List<StatusCount> contarPorStatus();

    @Query("SELECT COUNT(p) FROM Prospeccao p WHERE p.fechadaEm >= :desde")
    long countFechadasDesde(@Param("desde") java.time.Instant desde);

    /** Média em dias entre createdAt e fechadaEm para FECHADA_GANHA. Postgres-specific. */
    @Query(
            value =
                    "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (fechada_em - created_at))/86400.0), 0)"
                            + " FROM prospeccoes WHERE status = 'FECHADA_GANHA'"
                            + " AND fechada_em IS NOT NULL AND deleted_at IS NULL",
            nativeQuery = true)
    Double timeToCloseDiasMedio();

    interface StatusCount {
        ProspeccaoStatus getStatus();

        long getTotal();
    }
}
