package com.hubfeatcreators.domain.portal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessoriaBrandingRepository extends JpaRepository<AssessoriaBranding, UUID> {

    // Single-tenant: only one branding record exists; use findAll().stream().findFirst()
    // or findById(wellKnownId). Kept generic JpaRepository methods suffice.
}
