package com.hubfeatcreators.domain.match;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BriefingRepository extends JpaRepository<Briefing, UUID> {

    Optional<Briefing> findByProspeccaoId(UUID prospeccaoId);
}
