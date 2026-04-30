package com.hubfeatcreators.domain.assessoria;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessoriaRepository extends JpaRepository<Assessoria, UUID> {
  boolean existsBySlug(String slug);
  Optional<Assessoria> findBySlug(String slug);
}
