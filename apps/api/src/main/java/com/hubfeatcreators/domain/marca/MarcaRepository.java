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
            """
      SELECT m FROM Marca m
      WHERE m.deletedAt IS NULL
        AND (:nome IS NULL OR LOWER(m.nome) LIKE LOWER(CONCAT('%', :nome, '%')))
        AND (:segmento IS NULL OR m.segmento = :segmento)
      ORDER BY m.createdAt DESC
      """)
    Page<Marca> search(
            @Param("nome") String nome, @Param("segmento") String segmento, Pageable pageable);

    @Query("SELECT m FROM Marca m WHERE m.deletedAt IS NULL ORDER BY m.createdAt DESC")
    java.util.List<Marca> findAllActiveForExport();
}
