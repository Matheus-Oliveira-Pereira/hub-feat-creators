package com.hubfeatcreators.domain.admin;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformFeatureFlagRepository extends JpaRepository<PlatformFeatureFlag, String> {

    Optional<PlatformFeatureFlag> findByKey(String key);
}
