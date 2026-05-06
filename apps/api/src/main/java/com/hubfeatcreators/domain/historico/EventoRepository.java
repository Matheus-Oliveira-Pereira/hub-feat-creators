package com.hubfeatcreators.domain.historico;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoRepository extends JpaRepository<Evento, UUID> {

    // ─── OWNER: vê todos os eventos da assessoria ──────────────────────────

    @Query(
            value =
                    "SELECT * FROM eventos WHERE assessoria_id = :assessoriaId"
                            + " AND (:entidadeTipo IS NULL OR entidades_relacionadas @>"
                            + "   jsonb_build_array(jsonb_build_object('tipo',:entidadeTipo,'id',:entidadeId::text)))"
                            + " AND (:tipo IS NULL OR tipo = :tipo)"
                            + " AND (ts < :cursorTs OR (ts = :cursorTs AND id < :cursorId))"
                            + " ORDER BY ts DESC, id DESC LIMIT :lim",
            nativeQuery = true)
    List<Evento> findOwner(
            @Param("assessoriaId") UUID assessoriaId,
            @Param("entidadeTipo") String entidadeTipo,
            @Param("entidadeId") String entidadeId,
            @Param("tipo") String tipo,
            @Param("cursorTs") Instant cursorTs,
            @Param("cursorId") UUID cursorId,
            @Param("lim") int lim);

    @Query(
            value =
                    "SELECT * FROM eventos WHERE assessoria_id = :assessoriaId"
                            + " AND (:entidadeTipo IS NULL OR entidades_relacionadas @>"
                            + "   jsonb_build_array(jsonb_build_object('tipo',:entidadeTipo,'id',:entidadeId::text)))"
                            + " AND (:tipo IS NULL OR tipo = :tipo)"
                            + " ORDER BY ts DESC, id DESC LIMIT :lim",
            nativeQuery = true)
    List<Evento> findOwnerFirst(
            @Param("assessoriaId") UUID assessoriaId,
            @Param("entidadeTipo") String entidadeTipo,
            @Param("entidadeId") String entidadeId,
            @Param("tipo") String tipo,
            @Param("lim") int lim);

    // ─── ASSESSOR: só eventos onde autor_id = userId ───────────────────────

    @Query(
            value =
                    "SELECT * FROM eventos WHERE assessoria_id = :assessoriaId AND autor_id = :userId"
                            + " AND (:entidadeTipo IS NULL OR entidades_relacionadas @>"
                            + "   jsonb_build_array(jsonb_build_object('tipo',:entidadeTipo,'id',:entidadeId::text)))"
                            + " AND (:tipo IS NULL OR tipo = :tipo)"
                            + " AND (ts < :cursorTs OR (ts = :cursorTs AND id < :cursorId))"
                            + " ORDER BY ts DESC, id DESC LIMIT :lim",
            nativeQuery = true)
    List<Evento> findAssessor(
            @Param("assessoriaId") UUID assessoriaId,
            @Param("userId") UUID userId,
            @Param("entidadeTipo") String entidadeTipo,
            @Param("entidadeId") String entidadeId,
            @Param("tipo") String tipo,
            @Param("cursorTs") Instant cursorTs,
            @Param("cursorId") UUID cursorId,
            @Param("lim") int lim);

    @Query(
            value =
                    "SELECT * FROM eventos WHERE assessoria_id = :assessoriaId AND autor_id = :userId"
                            + " AND (:entidadeTipo IS NULL OR entidades_relacionadas @>"
                            + "   jsonb_build_array(jsonb_build_object('tipo',:entidadeTipo,'id',:entidadeId::text)))"
                            + " AND (:tipo IS NULL OR tipo = :tipo)"
                            + " ORDER BY ts DESC, id DESC LIMIT :lim",
            nativeQuery = true)
    List<Evento> findAssessorFirst(
            @Param("assessoriaId") UUID assessoriaId,
            @Param("userId") UUID userId,
            @Param("entidadeTipo") String entidadeTipo,
            @Param("entidadeId") String entidadeId,
            @Param("tipo") String tipo,
            @Param("lim") int lim);
}
