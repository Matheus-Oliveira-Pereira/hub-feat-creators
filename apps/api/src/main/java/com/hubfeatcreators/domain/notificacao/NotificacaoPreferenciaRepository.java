package com.hubfeatcreators.domain.notificacao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacaoPreferenciaRepository
        extends JpaRepository<NotificacaoPreferencia, NotificacaoPreferencia.PK> {

    List<NotificacaoPreferencia> findByUsuarioId(UUID usuarioId);

    Optional<NotificacaoPreferencia> findByUsuarioIdAndTipoAndCanal(
            UUID usuarioId, NotificacaoTipo tipo, NotificacaoCanal canal);
}
