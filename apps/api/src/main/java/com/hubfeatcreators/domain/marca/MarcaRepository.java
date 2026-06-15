package com.hubfeatcreators.domain.marca;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarcaRepository extends JpaRepository<Marca, UUID> {

    Optional<Marca> findByIdAndDeletedAtIsNull(UUID id);

    @Query(
            value =
                    """
      SELECT * FROM marcas
      WHERE deleted_at IS NULL
        AND (:nome IS NULL OR LOWER(nome) LIKE LOWER('%' || CAST(:nome AS text) || '%'))
        AND (:segmento IS NULL OR segmento = :segmento)
      ORDER BY created_at DESC
      """,
            countQuery =
                    """
      SELECT COUNT(*) FROM marcas
      WHERE deleted_at IS NULL
        AND (:nome IS NULL OR LOWER(nome) LIKE LOWER('%' || CAST(:nome AS text) || '%'))
        AND (:segmento IS NULL OR segmento = :segmento)
      """,
            nativeQuery = true)
    Page<Marca> search(
            @Param("nome") String nome, @Param("segmento") String segmento, Pageable pageable);

    @Query("SELECT m FROM Marca m WHERE m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    java.util.List<Marca> findAllActiveForExport();

    @Query(
            """
      SELECT m FROM Marca m
      WHERE m.deletedAt IS NULL
        AND LOWER(m.nome) = LOWER(:nome)
      """)
    java.util.Optional<Marca> findByNomeIgnoreCase(@Param("nome") String nome);
}
