package com.hubfeatcreators.domain.notificacao;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificacaoRepository extends JpaRepository<Notificacao, UUID> {

    @Query("""
        SELECT n FROM Notificacao n
        WHERE n.assessoriaId = :assessoriaId
          AND n.usuarioId = :usuarioId
          AND (:tipo IS NULL OR n.tipo = :tipo)
          AND (:apenasNaoLidas = FALSE OR n.lidaEm IS NULL)
        ORDER BY n.createdAt DESC
        """)
    Page<Notificacao> findFiltered(
            UUID assessoriaId,
            UUID usuarioId,
            NotificacaoTipo tipo,
            boolean apenasNaoLidas,
            Pageable pageable);

    long countByAssessoriaIdAndUsuarioIdAndLidaEmIsNull(UUID assessoriaId, UUID usuarioId);

    @Query("SELECT COUNT(n) FROM Notificacao n WHERE n.assessoriaId = :assessoriaId AND n.lidaEm IS NULL")
    long countNaoLidasByAssessoriaId(UUID assessoriaId);

    Optional<Notificacao> findByIdAndAssessoriaIdAndUsuarioId(UUID id, UUID assessoriaId, UUID usuarioId);

    @Modifying
    @Query("""
        UPDATE Notificacao n SET n.lidaEm = :agora
        WHERE n.assessoriaId = :assessoriaId
          AND n.usuarioId = :usuarioId
          AND n.lidaEm IS NULL
        """)
    int marcarTodasLidas(UUID assessoriaId, UUID usuarioId, Instant agora);

    @Query("""
        SELECT n FROM Notificacao n
        WHERE n.assessoriaId = :assessoriaId
          AND n.usuarioId = :usuarioId
          AND n.tipo = :tipo
          AND n.alvoId = :alvoId
          AND n.lidaEm IS NULL
        ORDER BY n.createdAt DESC
        """)
    Page<Notificacao> findPendingForDedupe(UUID assessoriaId, UUID usuarioId, NotificacaoTipo tipo, UUID alvoId, Pageable pageable);
}
