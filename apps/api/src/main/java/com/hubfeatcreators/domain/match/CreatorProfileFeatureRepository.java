package com.hubfeatcreators.domain.match;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreatorProfileFeatureRepository
        extends JpaRepository<CreatorProfileFeature, UUID> {

    Optional<CreatorProfileFeature> findByInfluenciadorId(UUID influenciadorId);

    List<CreatorProfileFeature> findByInfluenciadorIdIn(Collection<UUID> influenciadorIds);
}
