package com.hubfeatcreators.domain.tarefa;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TarefaRepository extends JpaRepository<Tarefa, UUID> {

  @Query("""
      SELECT t FROM Tarefa t WHERE t.assessoriaId = :assessoriaId
        AND (:status IS NULL OR t.status = :status)
        AND (:prioridade IS NULL OR t.prioridade = :prioridade)
        AND (:responsavelId IS NULL OR t.responsavelId = :responsavelId)
        AND (:prazoFiltro = 'VENCIDAS' AND t.prazo < :agora AND t.status NOT IN ('FEITA','CANCELADA')
             OR :prazoFiltro = 'HOJE' AND t.prazo >= :inicioDia AND t.prazo < :fimDia AND t.status NOT IN ('FEITA','CANCELADA')
             OR :prazoFiltro = 'SEMANA' AND t.prazo >= :agora AND t.prazo < :fimSemana
             OR :prazoFiltro = 'FUTURAS' AND t.prazo >= :fimSemana
             OR :prazoFiltro IS NULL)
      """)
  Page<Tarefa> findAllFiltered(
      @Param("assessoriaId") UUID assessoriaId,
      @Param("status") TarefaStatus status,
      @Param("prioridade") TarefaPrioridade prioridade,
      @Param("responsavelId") UUID responsavelId,
      @Param("prazoFiltro") String prazoFiltro,
      @Param("agora") Instant agora,
      @Param("inicioDia") Instant inicioDia,
      @Param("fimDia") Instant fimDia,
      @Param("fimSemana") Instant fimSemana,
      Pageable pageable);

  /** Contagem de tarefas vencidas + hoje para badge in-app. */
  @Query("""
      SELECT COUNT(t) FROM Tarefa t WHERE t.assessoriaId = :assessoriaId
        AND t.responsavelId = :usuarioId
        AND t.status NOT IN ('FEITA','CANCELADA')
        AND t.prazo < :fimDia
      """)
  long countAlerta(
      @Param("assessoriaId") UUID assessoriaId,
      @Param("usuarioId") UUID usuarioId,
      @Param("fimDia") Instant fimDia);

  /** Tarefas para digest diário: vencidas + hoje + próximas 3 da semana. */
  @Query("""
      SELECT t FROM Tarefa t WHERE t.assessoriaId = :assessoriaId
        AND t.status NOT IN ('FEITA','CANCELADA')
        AND t.prazo < :fimSemana
      ORDER BY t.prazo ASC
      """)
  List<Tarefa> findParaDigest(
      @Param("assessoriaId") UUID assessoriaId,
      @Param("fimSemana") Instant fimSemana);

  /** Tarefas vinculadas a uma entidade específica. */
  @Query("""
      SELECT t FROM Tarefa t WHERE t.assessoriaId = :assessoriaId
        AND t.entidadeTipo = :tipo AND t.entidadeId = :entidadeId
      ORDER BY t.prazo ASC
      """)
  List<Tarefa> findByEntidade(
      @Param("assessoriaId") UUID assessoriaId,
      @Param("tipo") EntidadeTipo tipo,
      @Param("entidadeId") UUID entidadeId);

  @Query(value = """
      SELECT COUNT(*) FROM tarefas WHERE assessoria_id = :assessoriaId
        AND status NOT IN ('FEITA','CANCELADA')
        AND prazo < :agora AND deleted_at IS NULL
      """, nativeQuery = true)
  long countVencidas(@Param("assessoriaId") UUID assessoriaId, @Param("agora") Instant agora);
}
