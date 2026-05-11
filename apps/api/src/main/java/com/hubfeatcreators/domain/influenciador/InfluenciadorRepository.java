package com.hubfeatcreators.domain.influenciador;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InfluenciadorRepository extends JpaRepository<Influenciador, UUID> {

    Optional<Influenciador> findByIdAndDeletedAtIsNull(UUID id);

    @Query(
            """
      SELECT i FROM Influenciador i
      WHERE i.deletedAt IS NULL
        AND (:nome IS NULL OR LOWER(i.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
        AND (:nicho IS NULL OR i.nicho = :nicho)
      ORDER BY i.createdAt DESC
      """)
    Page<Influenciador> search(
            @Param("nome") String nome, @Param("nicho") String nicho, Pageable pageable);

    @Query("SELECT i FROM Influenciador i WHERE i.deletedAt IS NULL ORDER BY i.createdAt DESC")
    java.util.List<Influenciador> findAllActiveForExport();

    @Query(
            value = """
      SELECT * FROM influenciadores
      WHERE assessoria_id = :assessoriaId
        AND deleted_at IS NULL
        AND handles->>:handleKey = :handleValue
      LIMIT 1
      """,
            nativeQuery = true)
    Optional<Influenciador> findByHandleAndAssessoria(
            @Param("assessoriaId") UUID assessoriaId,
            @Param("handleKey") String handleKey,
            @Param("handleValue") String handleValue);

}
