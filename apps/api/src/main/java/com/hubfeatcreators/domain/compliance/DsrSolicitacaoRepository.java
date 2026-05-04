package com.hubfeatcreators.domain.compliance;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DsrSolicitacaoRepository extends JpaRepository<DsrSolicitacao, UUID> {

    List<DsrSolicitacao> findByAssessoriaIdAndStatus(UUID assessoriaId, DsrSolicitacao.StatusDsr status);

    @Query("SELECT d FROM DsrSolicitacao d WHERE d.status = 'PENDENTE' AND d.prazoLegalEm < :limite")
    List<DsrSolicitacao> findVencendoAntes(@Param("limite") Instant limite);
}
