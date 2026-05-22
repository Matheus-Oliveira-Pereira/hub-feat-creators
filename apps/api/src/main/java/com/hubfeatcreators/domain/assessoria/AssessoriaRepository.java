package com.hubfeatcreators.domain.assessoria;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AssessoriaRepository extends JpaRepository<Assessoria, UUID> {
    boolean existsBySlug(String slug);

    Optional<Assessoria> findBySlug(String slug);

    @Query("SELECT a FROM Assessoria a WHERE a.deletedAt IS NULL ORDER BY a.createdAt DESC")
    Page<Assessoria> findAllActive(Pageable pageable);
}
