package com.hubfeatcreators.infra.audit;

import java.util.Map;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

  private final AuditLogRepository repository;

  public AuditLogService(AuditLogRepository repository) {
    this.repository = repository;
  }

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void log(
      UUID assessoriaId,
      UUID usuarioId,
      String entidade,
      UUID entidadeId,
      AuditLog.Acao acao,
      Map<String, Object> payload) {
    repository.save(new AuditLog(assessoriaId, usuarioId, entidade, entidadeId, acao, payload));
  }
}
