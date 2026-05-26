package com.hubfeatcreators.domain.relatorio;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RelatorioSalvoRepository extends JpaRepository<RelatorioSalvo, UUID> {

    List<RelatorioSalvo> findByUsuarioIdOrderByCreatedAtDesc(UUID usuarioId);

    Optional<RelatorioSalvo> findByIdAndUsuarioId(UUID id, UUID usuarioId);
}
