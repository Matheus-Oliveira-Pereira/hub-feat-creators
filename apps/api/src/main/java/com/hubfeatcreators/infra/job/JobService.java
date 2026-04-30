package com.hubfeatcreators.infra.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {

  private static final Logger log = LoggerFactory.getLogger(JobService.class);

  private final JobRepository repository;
  private final ObjectMapper mapper;

  public JobService(JobRepository repository, ObjectMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Transactional
  public void enqueue(UUID assessoriaId, String tipo, Object payload, UUID idempotencyKey) {
    enqueue(assessoriaId, tipo, payload, idempotencyKey, Instant.now());
  }

  @Transactional
  public void enqueue(
      UUID assessoriaId,
      String tipo,
      Object payload,
      UUID idempotencyKey,
      Instant scheduledFor) {
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> payloadMap = mapper.convertValue(payload, Map.class);
      Job job = new Job(assessoriaId, tipo, payloadMap, idempotencyKey);
      job.setAgendadoPara(scheduledFor);
      job.setProximaTentativaEm(scheduledFor);
      repository.save(job);
    } catch (DataIntegrityViolationException e) {
      log.debug("Job idempotent skip: tipo={} key={}", tipo, idempotencyKey);
    }
  }
}
