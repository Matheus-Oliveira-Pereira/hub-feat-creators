package com.hubfeatcreators.domain.tarefa;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarefaComentarioRepository extends JpaRepository<TarefaComentario, UUID> {

    List<TarefaComentario> findByTarefaIdOrderByCreatedAtDesc(UUID tarefaId);
}
