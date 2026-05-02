package com.hubfeatcreators.domain.prospeccao;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProspeccaoEventoRepository extends JpaRepository<ProspeccaoEvento, UUID> {

  List<ProspeccaoEvento> findByProspeccaoIdOrderByCreatedAtDesc(UUID prospeccaoId);
}
