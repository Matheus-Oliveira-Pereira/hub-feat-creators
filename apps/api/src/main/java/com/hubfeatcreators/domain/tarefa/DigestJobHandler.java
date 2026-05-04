package com.hubfeatcreators.domain.tarefa;

import com.hubfeatcreators.infra.job.Job;
import com.hubfeatcreators.infra.job.JobHandler;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Processa job EMAIL_DIGEST: busca tarefas vencidas+hoje+próximas 3 da semana,
 * agrupa por responsável, envia digest. Respeita opt-out via usuario_preferencias.
 * Idempotente via idempotency_key do job.
 */
@Component("EMAIL_DIGEST")
public class DigestJobHandler implements JobHandler {

  private static final Logger log = LoggerFactory.getLogger(DigestJobHandler.class);
  private static final ZoneId TZ_BR = ZoneId.of("America/Sao_Paulo");
  private static final int MAX_ATRASADAS = 20;

  private final TarefaRepository tarefaRepo;
  private final UsuarioPreferenciaRepository prefRepo;
  private final JavaMailSender mailSender;
  private final MeterRegistry meterRegistry;

  public DigestJobHandler(
      TarefaRepository tarefaRepo,
      UsuarioPreferenciaRepository prefRepo,
      JavaMailSender mailSender,
      MeterRegistry meterRegistry) {
    this.tarefaRepo = tarefaRepo;
    this.prefRepo = prefRepo;
    this.mailSender = mailSender;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public void handle(Job job) {
    Map<String, Object> payload = job.getPayload();
    UUID assessoriaId = UUID.fromString((String) payload.get("assessoriaId"));
    String data = (String) payload.get("data");

    ZonedDateTime inicioDia = ZonedDateTime.now(TZ_BR).toLocalDate().atStartOfDay(TZ_BR);
    Instant fimSemana = inicioDia.plusDays(7).toInstant();

    List<Tarefa> tarefas = tarefaRepo.findParaDigest(assessoriaId, fimSemana);
    if (tarefas.isEmpty()) {
      log.info("digest.handler.skip assessoriaId={} data={} motivo=sem_tarefas", assessoriaId, data);
      return;
    }

    Map<UUID, List<Tarefa>> porResponsavel = tarefas.stream()
        .collect(Collectors.groupingBy(Tarefa::getResponsavelId));

    porResponsavel.forEach((responsavelId, tarefasDoResponsavel) -> {
      boolean digestEnabled = prefRepo.findById(responsavelId)
          .map(UsuarioPreferencia::isDigestDiarioEnabled)
          .orElse(true);

      if (!digestEnabled) {
        log.info("digest.handler.optout responsavelId={}", responsavelId);
        return;
      }

      try {
        enviarDigest(responsavelId, tarefasDoResponsavel, inicioDia.toInstant(), data);
        Counter.builder("digest_enviado_total").register(meterRegistry).increment();
      } catch (Exception e) {
        log.error("digest.handler.email.error responsavelId={} msg={}", responsavelId, e.getMessage(), e);
        Counter.builder("digest_falha_total").register(meterRegistry).increment();
      }
    });
  }

  private void enviarDigest(UUID responsavelId, List<Tarefa> tarefas, Instant agora, String data) {
    Instant fimDia = ZonedDateTime.now(TZ_BR).toLocalDate().atStartOfDay(TZ_BR).plusDays(1).toInstant();

    List<Tarefa> atrasadas = tarefas.stream()
        .filter(t -> t.getPrazo().isBefore(agora))
        .limit(MAX_ATRASADAS)
        .toList();

    List<Tarefa> hoje = tarefas.stream()
        .filter(t -> !t.getPrazo().isBefore(agora) && t.getPrazo().isBefore(fimDia))
        .toList();

    List<Tarefa> proximas = tarefas.stream()
        .filter(t -> !t.getPrazo().isBefore(fimDia))
        .limit(3)
        .toList();

    if (atrasadas.isEmpty() && hoje.isEmpty() && proximas.isEmpty()) return;

    var sb = new StringBuilder();
    sb.append("Seu resumo de tarefas — ").append(data).append("\n\n");

    if (!atrasadas.isEmpty()) {
      sb.append("⚠ ATRASADAS (").append(atrasadas.size()).append(")\n");
      atrasadas.forEach(t -> sb.append("  • ").append(t.getTitulo()).append(" (prazo: ").append(t.getPrazo()).append(")\n"));
      sb.append("\n");
    }

    if (!hoje.isEmpty()) {
      sb.append("📅 HOJE\n");
      hoje.forEach(t -> sb.append("  • ").append(t.getTitulo()).append("\n"));
      sb.append("\n");
    }

    if (!proximas.isEmpty()) {
      sb.append("🗓 PRÓXIMAS (esta semana)\n");
      proximas.forEach(t -> sb.append("  • ").append(t.getTitulo()).append(" (").append(t.getPrazo()).append(")\n"));
    }

    SimpleMailMessage msg = new SimpleMailMessage();
    msg.setSubject("Suas tarefas de hoje — " + data);
    msg.setText(sb.toString());
    // TODO (PRD-004): resolver e-mail do usuário via UserRepository quando e-mail estiver disponível.
    // Por ora o job é enfileirado sem destinatário — integrará com PRD-004.
    log.info("digest.send responsavelId={} atrasadas={} hoje={} proximas={}",
        responsavelId, atrasadas.size(), hoje.size(), proximas.size());
  }
}
