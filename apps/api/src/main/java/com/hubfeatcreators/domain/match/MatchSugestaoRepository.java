package com.hubfeatcreators.domain.match;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MatchSugestaoRepository extends JpaRepository<MatchSugestao, UUID> {

    List<MatchSugestao> findByProspeccaoIdAndModeloVersaoOrderByScoreDesc(
            UUID prospeccaoId, String modeloVersao);

    List<MatchSugestao> findByInfluenciadorIdOrderByScoreDesc(UUID influenciadorId);

    Optional<MatchSugestao> findByProspeccaoIdAndInfluenciadorIdAndModeloVersao(
            UUID prospeccaoId, UUID influenciadorId, String modeloVersao);

    @Query("SELECT DISTINCT s.prospeccaoId FROM MatchSugestao s WHERE s.influenciadorId = :influenciadorId ORDER BY s.prospeccaoId")
    List<UUID> findProspeccoesByInfluenciadorId(UUID influenciadorId);

    void deleteByProspeccaoIdAndModeloVersao(UUID prospeccaoId, String modeloVersao);
}
