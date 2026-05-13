package com.hubfeatcreators.domain.social;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialSnapshotRepository extends JpaRepository<SocialSnapshot, UUID> {

    Optional<SocialSnapshot> findBySocialAccountIdAndDia(UUID socialAccountId, LocalDate dia);

    List<SocialSnapshot> findBySocialAccountIdOrderByDiaDesc(
            UUID socialAccountId, Pageable pageable);
}
