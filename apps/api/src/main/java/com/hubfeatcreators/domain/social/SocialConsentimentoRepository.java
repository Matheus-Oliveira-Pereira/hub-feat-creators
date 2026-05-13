package com.hubfeatcreators.domain.social;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialConsentimentoRepository extends JpaRepository<SocialConsentimento, UUID> {

    List<SocialConsentimento> findByInfluenciadorId(UUID influenciadorId);

    Optional<SocialConsentimento>
            findTopByInfluenciadorIdAndPlataformaAndRevogadoEmIsNullOrderByDadoEmDesc(
                    UUID influenciadorId, String plataforma);
}
