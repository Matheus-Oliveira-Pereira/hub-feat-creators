package com.hubfeatcreators.domain.portal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AssessoriaBrandingRepository extends JpaRepository<AssessoriaBranding, UUID> {

    @Query(
            "SELECT b FROM AssessoriaBranding b JOIN Assessoria a ON b.assessoriaId = a.id WHERE a.slug = :slug")
    Optional<AssessoriaBranding> findByAssessoriaSlug(String slug);
}
