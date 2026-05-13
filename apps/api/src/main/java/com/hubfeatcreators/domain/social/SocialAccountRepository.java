package com.hubfeatcreators.domain.social;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {

    List<SocialAccount> findByInfluenciadorId(UUID influenciadorId);

    List<SocialAccount> findByAssessoriaIdAndInfluenciadorId(
            UUID assessoriaId, UUID influenciadorId);

    Optional<SocialAccount> findByPlataformaAndExternalUserId(
            String plataforma, String externalUserId);

    @Query("SELECT a FROM SocialAccount a WHERE a.status = 'ATIVA' AND a.expiresAt < :threshold")
    List<SocialAccount> findDueForRefresh(Instant threshold);

    @Query("SELECT a FROM SocialAccount a WHERE a.status = 'ATIVA'")
    List<SocialAccount> findAllAtiva();
}
