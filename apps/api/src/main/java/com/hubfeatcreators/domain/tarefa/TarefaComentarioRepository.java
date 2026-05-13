package com.hubfeatcreators.domain.tarefa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaComentarioRepository extends JpaRepository<TarefaComentario, UUID> {

    List<TarefaComentario> findByTarefaIdOrderByCreatedAtDesc(UUID tarefaId);

    List<TarefaComentario> findByTarefaIdAndAssessoriaIdOrderByCreatedAtAsc(
            UUID tarefaId, UUID assessoriaId);

    default List<TarefaComentario> findByTarefaIdAndAssessoriaId(UUID tarefaId, UUID assessoriaId) {
        return findByTarefaIdAndAssessoriaIdOrderByCreatedAtAsc(tarefaId, assessoriaId);
    }
}
