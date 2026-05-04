package com.hubfeatcreators.domain.tarefa;

public enum TarefaStatus {
  TODO, EM_ANDAMENTO, FEITA, CANCELADA;

  public boolean isTerminal() {
    return this == FEITA || this == CANCELADA;
  }
}
