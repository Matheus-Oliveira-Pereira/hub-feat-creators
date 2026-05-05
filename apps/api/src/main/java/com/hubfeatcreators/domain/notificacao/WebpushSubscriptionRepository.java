package com.hubfeatcreators.domain.notificacao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface WebpushSubscriptionRepository extends JpaRepository<WebpushSubscription, UUID> {

    List<WebpushSubscription> findByUsuarioIdAndAtivaTrue(UUID usuarioId);

    Optional<WebpushSubscription> findByEndpoint(String endpoint);

    @Modifying
    @Query("UPDATE WebpushSubscription w SET w.ativa = false WHERE w.endpoint = :endpoint")
    void markInativa(String endpoint);
}
