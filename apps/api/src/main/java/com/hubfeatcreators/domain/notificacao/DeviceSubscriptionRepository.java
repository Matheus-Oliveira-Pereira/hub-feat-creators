package com.hubfeatcreators.domain.notificacao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceSubscriptionRepository extends JpaRepository<DeviceSubscription, UUID> {

    List<DeviceSubscription> findByUserIdAndAtivaTrue(UUID userId);

    Optional<DeviceSubscription> findByToken(String token);

    boolean existsByUserIdAndToken(UUID userId, String token);
}
