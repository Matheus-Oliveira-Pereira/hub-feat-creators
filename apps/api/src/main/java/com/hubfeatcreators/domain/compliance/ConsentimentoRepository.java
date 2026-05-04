package com.hubfeatcreators.domain.compliance;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentimentoRepository extends JpaRepository<Consentimento, UUID> {
    List<Consentimento> findByTitularTipoAndTitularId(String titularTipo, UUID titularId);
    Optional<Consentimento> findByTitularTipoAndTitularIdAndFinalidade(String titularTipo, UUID titularId, String finalidade);
}
