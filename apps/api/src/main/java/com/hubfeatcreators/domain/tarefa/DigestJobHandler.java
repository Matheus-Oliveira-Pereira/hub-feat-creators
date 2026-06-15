package com.hubfeatcreators.domain.tarefa;

import com.hubfeatcreators.domain.email.SystemEmailConfig;
import com.hubfeatcreators.domain.email.SystemEmailConfigService;
import com.hubfeatcreators.domain.usuario.UsuarioRepository;
import com.hubfeatcreators.infra.job.Job;
import com.hubfeatcreators.infra.job.JobHandler;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Processa job EMAIL_DIGEST: busca tarefas vencidas+hoje+próximas 7 dias, agrupa por responsável,
 * envia digest. Respeita opt-out via usuario_preferencias. Idempotente via idempotency_key do job.
 */
@Component("EMAIL_DIGEST")
public class DigestJobHandler implements JobHandler {

    private static final Logger log = LoggerFactory.getLogger(DigestJobHandler.class);
    private static final ZoneId TZ_BR = ZoneId.of("America/Sao_Paulo");
    private static final int MAX_ATRASADAS = 20;

    private final TarefaRepository tarefaRepo;
    private final UsuarioPreferenciaRepository prefRepo;
    private final UsuarioRepository usuarioRepo;
    private final SystemEmailConfigService systemConfigService;
    private final MeterRegistry meterRegistry;

    public DigestJobHandler(
            TarefaRepository tarefaRepo,
            UsuarioPreferenciaRepository prefRepo,
            UsuarioRepository usuarioRepo,
            SystemEmailConfigService systemConfigService,
            MeterRegistry meterRegistry) {
        this.tarefaRepo = tarefaRepo;
        this.prefRepo = prefRepo;
        this.usuarioRepo = usuarioRepo;
        this.systemConfigService = systemConfigService;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void handle(Job job) {
        Map<String, Object> payload = job.getPayload();
        String data = (String) payload.get("data");

        Optional<JavaMailSender> senderOpt = systemConfigService.getSender();
        if (senderOpt.isEmpty()) {
            log.warn("digest.handler.skip motivo=email_nao_configurado");
            return;
        }
        JavaMailSender sender = senderOpt.get();

        SystemEmailConfig cfg = systemConfigService.getEffectiveConfig().orElseThrow();

        ZonedDateTime inicioDia = ZonedDateTime.now(TZ_BR).toLocalDate().atStartOfDay(TZ_BR);
        Instant fimSemana = inicioDia.plusDays(7).toInstant();

        List<Tarefa> tarefas = tarefaRepo.findParaDigest(fimSemana);
        if (tarefas.isEmpty()) {
            log.info("digest.handler.skip data={} motivo=sem_tarefas", data);
            return;
        }

        Map<UUID, List<Tarefa>> porResponsavel =
                tarefas.stream().collect(Collectors.groupingBy(Tarefa::getResponsavelId));

        porResponsavel.forEach(
                (responsavelId, tarefasDoResponsavel) -> {
                    boolean digestEnabled =
                            prefRepo.findById(responsavelId)
                                    .map(UsuarioPreferencia::isDigestDiarioEnabled)
                                    .orElse(true);

                    if (!digestEnabled) {
                        log.info("digest.handler.optout responsavelId={}", responsavelId);
                        return;
                    }

                    usuarioRepo
                            .findById(responsavelId)
                            .ifPresentOrElse(
                                    usuario -> {
                                        try {
                                            enviarDigest(
                                                    sender,
                                                    cfg,
                                                    responsavelId,
                                                    usuario.getEmail(),
                                                    tarefasDoResponsavel,
                                                    inicioDia.toInstant(),
                                                    data);
                                            Counter.builder("digest_enviado_total")
                                                    .register(meterRegistry)
                                                    .increment();
                                        } catch (Exception e) {
                                            log.error(
                                                    "digest.handler.email.error responsavelId={} msg={}",
                                                    responsavelId,
                                                    e.getMessage(),
                                                    e);
                                            Counter.builder("digest_falha_total")
                                                    .register(meterRegistry)
                                                    .increment();
                                        }
                                    },
                                    () ->
                                            log.warn(
                                                    "digest.handler.usuario_nao_encontrado responsavelId={}",
                                                    responsavelId));
                });
    }

    private void enviarDigest(
            JavaMailSender sender,
            SystemEmailConfig cfg,
            UUID responsavelId,
            String email,
            List<Tarefa> tarefas,
            Instant agora,
            String data) {
        Instant fimDia =
                ZonedDateTime.now(TZ_BR).toLocalDate().atStartOfDay(TZ_BR).plusDays(1).toInstant();

        List<Tarefa> atrasadas =
                tarefas.stream()
                        .filter(t -> t.getPrazo().isBefore(agora))
                        .limit(MAX_ATRASADAS)
                        .toList();
        List<Tarefa> hoje =
                tarefas.stream()
                        .filter(t -> !t.getPrazo().isBefore(agora) && t.getPrazo().isBefore(fimDia))
                        .toList();
        List<Tarefa> proximas =
                tarefas.stream().filter(t -> !t.getPrazo().isBefore(fimDia)).limit(3).toList();

        if (atrasadas.isEmpty() && hoje.isEmpty() && proximas.isEmpty()) return;

        var sb = new StringBuilder();
        sb.append("Seu resumo de tarefas — ").append(data).append("\n\n");
        if (!atrasadas.isEmpty()) {
            sb.append("ATRASADAS (").append(atrasadas.size()).append(")\n");
            atrasadas.forEach(
                    t ->
                            sb.append("  - ")
                                    .append(t.getTitulo())
                                    .append(" (prazo: ")
                                    .append(t.getPrazo())
                                    .append(")\n"));
            sb.append("\n");
        }
        if (!hoje.isEmpty()) {
            sb.append("HOJE\n");
            hoje.forEach(t -> sb.append("  - ").append(t.getTitulo()).append("\n"));
            sb.append("\n");
        }
        if (!proximas.isEmpty()) {
            sb.append("PROXIMAS (esta semana)\n");
            proximas.forEach(
                    t ->
                            sb.append("  - ")
                                    .append(t.getTitulo())
                                    .append(" (")
                                    .append(t.getPrazo())
                                    .append(")\n"));
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(cfg.getFromAddress());
        msg.setTo(email);
        msg.setSubject("Suas tarefas de hoje — " + data);
        msg.setText(sb.toString());
        sender.send(msg);

        log.info(
                "digest.send responsavelId={} email={} atrasadas={} hoje={} proximas={}",
                responsavelId,
                email,
                atrasadas.size(),
                hoje.size(),
                proximas.size());
    }
}
