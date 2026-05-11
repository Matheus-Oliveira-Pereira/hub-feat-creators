package com.hubfeatcreators.domain.relatorio;

import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.web.BusinessException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RelatorioService {

    private static final ZoneId BRT = ZoneId.of("America/Sao_Paulo");
    private static final int MAX_MONTHS = 24;

    private final JdbcTemplate jdbc;
    private final RelatorioSalvoRepository savedRepo;

    public RelatorioService(JdbcTemplate jdbc, RelatorioSalvoRepository savedRepo) {
        this.jdbc = jdbc;
        this.savedRepo = savedRepo;
    }

    // ─── DTOs ────────────────────────────────────────────────────────────────

    public record FunilItem(
            String status,
            long qtd, long qtdAnterior, double deltaPercent,
            long valorTotalCentavos, double tempoMedioDias) {}

    public record FunilResult(List<FunilItem> itens, String periodoAtual, String periodoAnterior) {}

    public record AssessorItem(
            String assessorId, String assessorEmail,
            long abertas, long ganhas, long perdidas,
            long ticketMedioCentavos, double tempoMedioFechamentoDias,
            long abertasAnterior, long ganhasAnterior, long perdidasAnterior) {}

    public record PerformanceResult(List<AssessorItem> assessores, String periodoAtual, String periodoAnterior) {}

    public record TarefaSlaItem(
            String responsavelId, String responsavelEmail,
            long total, long noPrazo, long atrasadas, double atrasoMedioHoras,
            long totalAnterior) {}

    public record TarefaSlaResult(List<TarefaSlaItem> assessores, String periodoAtual, String periodoAnterior) {}

    // ─── Funil de Prospecção ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FunilResult funil(UUID assessoriaId, Instant from, Instant to, UUID assessorId) {
        validate(from, to);

        Instant[] prev = previousPeriod(from, to);

        List<Map<String, Object>> current = queryFunil(assessoriaId, from, to, assessorId);
        List<Map<String, Object>> previous = queryFunil(assessoriaId, prev[0], prev[1], assessorId);

        List<FunilItem> itens = new ArrayList<>();
        List<String> allStatuses = List.of("NOVA", "CONTATADA", "NEGOCIANDO", "FECHADA_GANHA", "FECHADA_PERDIDA");

        for (String status : allStatuses) {
            Map<String, Object> cur = findByStatus(current, status);
            Map<String, Object> pre = findByStatus(previous, status);
            long qtd = cur != null ? toLong(cur.get("qtd")) : 0;
            long qtdAnterior = pre != null ? toLong(pre.get("qtd")) : 0;
            double delta = qtdAnterior == 0 ? (qtd > 0 ? 100.0 : 0.0) : ((qtd - qtdAnterior) * 100.0 / qtdAnterior);
            long valor = cur != null ? toLong(cur.get("valor_total_centavos")) : 0;
            double tempo = cur != null ? toDouble(cur.get("tempo_medio_dias")) : 0;
            itens.add(new FunilItem(status, qtd, qtdAnterior, delta, valor, tempo));
        }

        return new FunilResult(itens, fmtPeriod(from, to), fmtPeriod(prev[0], prev[1]));
    }

    private List<Map<String, Object>> queryFunil(UUID assessoriaId, Instant from, Instant to, UUID assessorId) {
        String sql = """
            SELECT
              status,
              COUNT(*) AS qtd,
              COALESCE(SUM(valor_estimado_centavos), 0) AS valor_total_centavos,
              COALESCE(AVG(EXTRACT(EPOCH FROM (updated_at - created_at)) / 86400.0)
                FILTER (WHERE status IN ('FECHADA_GANHA','FECHADA_PERDIDA')), 0) AS tempo_medio_dias
            FROM prospeccoes
            WHERE assessoria_id = ?
              AND deleted_at IS NULL
              AND created_at >= ? AND created_at < ?
              AND (? IS NULL OR assessor_responsavel_id = ?)
            GROUP BY status
            """;
        return jdbc.queryForList(sql, assessoriaId, from, to, assessorId, assessorId);
    }

    // ─── Performance Assessor ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PerformanceResult performanceAssessor(UUID assessoriaId, Instant from, Instant to) {
        validate(from, to);
        Instant[] prev = previousPeriod(from, to);

        List<Map<String, Object>> current = queryPerformance(assessoriaId, from, to);
        List<Map<String, Object>> previous = queryPerformance(assessoriaId, prev[0], prev[1]);

        List<Map<String, Object>> usuarios = jdbc.queryForList(
                "SELECT id, email FROM usuarios WHERE assessoria_id = ? AND deleted_at IS NULL", assessoriaId);

        List<AssessorItem> assessores = new ArrayList<>();
        for (Map<String, Object> row : current) {
            UUID aId = (UUID) row.get("assessor_id");
            String email = usuarios.stream()
                    .filter(u -> aId.equals(u.get("id")))
                    .map(u -> (String) u.get("email"))
                    .findFirst().orElse(aId.toString().substring(0, 8));

            Map<String, Object> pre = previous.stream()
                    .filter(r -> aId.equals(r.get("assessor_id")))
                    .findFirst().orElse(null);

            assessores.add(new AssessorItem(
                    aId.toString(), email,
                    toLong(row.get("abertas")), toLong(row.get("ganhas")), toLong(row.get("perdidas")),
                    toLong(row.get("ticket_medio_centavos")), toDouble(row.get("tempo_medio_fechamento_dias")),
                    pre != null ? toLong(pre.get("abertas")) : 0,
                    pre != null ? toLong(pre.get("ganhas")) : 0,
                    pre != null ? toLong(pre.get("perdidas")) : 0));
        }

        return new PerformanceResult(assessores, fmtPeriod(from, to), fmtPeriod(prev[0], prev[1]));
    }

    private List<Map<String, Object>> queryPerformance(UUID assessoriaId, Instant from, Instant to) {
        String sql = """
            SELECT
              assessor_responsavel_id AS assessor_id,
              COUNT(*) FILTER (WHERE status NOT IN ('FECHADA_GANHA','FECHADA_PERDIDA')) AS abertas,
              COUNT(*) FILTER (WHERE status = 'FECHADA_GANHA') AS ganhas,
              COUNT(*) FILTER (WHERE status = 'FECHADA_PERDIDA') AS perdidas,
              COALESCE(AVG(valor_estimado_centavos) FILTER (WHERE status = 'FECHADA_GANHA'), 0)::BIGINT AS ticket_medio_centavos,
              COALESCE(AVG(EXTRACT(EPOCH FROM (updated_at - created_at)) / 86400.0)
                FILTER (WHERE status IN ('FECHADA_GANHA','FECHADA_PERDIDA')), 0) AS tempo_medio_fechamento_dias
            FROM prospeccoes
            WHERE assessoria_id = ?
              AND deleted_at IS NULL
              AND created_at >= ? AND created_at < ?
            GROUP BY assessor_responsavel_id
            """;
        return jdbc.queryForList(sql, assessoriaId, from, to);
    }

    // ─── Tarefas SLA ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TarefaSlaResult tarefaSla(UUID assessoriaId, Instant from, Instant to, UUID responsavelId) {
        validate(from, to);
        Instant[] prev = previousPeriod(from, to);

        List<Map<String, Object>> current = querySla(assessoriaId, from, to, responsavelId);
        List<Map<String, Object>> previous = querySla(assessoriaId, prev[0], prev[1], responsavelId);

        List<Map<String, Object>> usuarios = jdbc.queryForList(
                "SELECT id, email FROM usuarios WHERE assessoria_id = ? AND deleted_at IS NULL", assessoriaId);

        List<TarefaSlaItem> assessores = new ArrayList<>();
        for (Map<String, Object> row : current) {
            UUID rId = (UUID) row.get("responsavel_id");
            String email = usuarios.stream()
                    .filter(u -> rId.equals(u.get("id")))
                    .map(u -> (String) u.get("email"))
                    .findFirst().orElse(rId.toString().substring(0, 8));

            long totalAnterior = previous.stream()
                    .filter(r -> rId.equals(r.get("responsavel_id")))
                    .map(r -> toLong(r.get("total")))
                    .findFirst().orElse(0L);

            assessores.add(new TarefaSlaItem(
                    rId.toString(), email,
                    toLong(row.get("total")), toLong(row.get("no_prazo")), toLong(row.get("atrasadas")),
                    toDouble(row.get("atraso_medio_horas")), totalAnterior));
        }

        return new TarefaSlaResult(assessores, fmtPeriod(from, to), fmtPeriod(prev[0], prev[1]));
    }

    private List<Map<String, Object>> querySla(UUID assessoriaId, Instant from, Instant to, UUID responsavelId) {
        String sql = """
            SELECT
              responsavel_id,
              COUNT(*) AS total,
              COUNT(*) FILTER (WHERE status = 'FEITA' AND concluida_em <= prazo) AS no_prazo,
              COUNT(*) FILTER (WHERE status NOT IN ('FEITA','CANCELADA') AND prazo < NOW()) AS atrasadas,
              COALESCE(
                AVG(EXTRACT(EPOCH FROM (COALESCE(concluida_em, NOW()) - prazo)) / 3600.0)
                  FILTER (WHERE prazo < COALESCE(concluida_em, NOW())),
                0
              ) AS atraso_medio_horas
            FROM tarefas
            WHERE assessoria_id = ?
              AND deleted_at IS NULL
              AND created_at >= ? AND created_at < ?
              AND (? IS NULL OR responsavel_id = ?)
            GROUP BY responsavel_id
            """;
        return jdbc.queryForList(sql, assessoriaId, from, to, responsavelId, responsavelId);
    }

    // ─── Export CSV ──────────────────────────────────────────────────────────

    public void exportFunilCsv(UUID assessoriaId, Instant from, Instant to, UUID assessorId, PrintWriter out) {
        writeBom(out);
        out.println("# tipo=FUNIL from=" + from + " to=" + to + (assessorId != null ? " assessor_id=" + assessorId : ""));
        out.println("status,qtd,qtd_anterior,delta_percent,valor_total_centavos,tempo_medio_dias");
        FunilResult r = funil(assessoriaId, from, to, assessorId);
        r.itens().forEach(i -> out.printf("%s,%d,%d,%.2f,%d,%.2f%n",
                i.status(), i.qtd(), i.qtdAnterior(), i.deltaPercent(),
                i.valorTotalCentavos(), i.tempoMedioDias()));
    }

    public void exportPerformanceCsv(UUID assessoriaId, Instant from, Instant to, PrintWriter out) {
        writeBom(out);
        out.println("# tipo=PERFORMANCE_ASSESSOR from=" + from + " to=" + to);
        out.println("assessor_id,assessor_email,abertas,ganhas,perdidas,ticket_medio_centavos,tempo_medio_fechamento_dias,abertas_anterior,ganhas_anterior,perdidas_anterior");
        PerformanceResult r = performanceAssessor(assessoriaId, from, to);
        r.assessores().forEach(i -> out.printf("%s,%s,%d,%d,%d,%d,%.2f,%d,%d,%d%n",
                i.assessorId(), i.assessorEmail(),
                i.abertas(), i.ganhas(), i.perdidas(),
                i.ticketMedioCentavos(), i.tempoMedioFechamentoDias(),
                i.abertasAnterior(), i.ganhasAnterior(), i.perdidasAnterior()));
    }

    public void exportSlavCsv(UUID assessoriaId, Instant from, Instant to, UUID responsavelId, PrintWriter out) {
        writeBom(out);
        out.println("# tipo=TAREFAS_SLA from=" + from + " to=" + to);
        out.println("responsavel_id,responsavel_email,total,no_prazo,atrasadas,atraso_medio_horas,total_anterior");
        TarefaSlaResult r = tarefaSla(assessoriaId, from, to, responsavelId);
        r.assessores().forEach(i -> out.printf("%s,%s,%d,%d,%d,%.2f,%d%n",
                i.responsavelId(), i.responsavelEmail(),
                i.total(), i.noPrazo(), i.atrasadas(),
                i.atrasoMedioHoras(), i.totalAnterior()));
    }

    private void writeBom(PrintWriter out) {
        out.write('﻿');
    }

    // ─── Relatórios Salvos ───────────────────────────────────────────────────

    @Transactional
    public RelatorioSalvo salvar(AuthPrincipal principal, String nome, String tipo, Map<String, Object> filtros) {
        validateTipo(tipo);
        return savedRepo.save(new RelatorioSalvo(principal.assessoriaId(), principal.usuarioId(), nome, tipo, filtros));
    }

    @Transactional(readOnly = true)
    public List<RelatorioSalvo> listarSalvos(AuthPrincipal principal) {
        return savedRepo.findByAssessoriaIdOrderByCreatedAtDesc(principal.assessoriaId());
    }

    @Transactional
    public void deletarSalvo(AuthPrincipal principal, UUID id) {
        savedRepo.findByIdAndAssessoriaId(id, principal.assessoriaId())
                .ifPresentOrElse(
                        savedRepo::delete,
                        () -> { throw BusinessException.notFound(); });
    }

    // ─── MV Refresh ──────────────────────────────────────────────────────────

    public void refreshMvs() {
        jdbc.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_funil_prospeccao");
        jdbc.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_tarefa_sla");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void validate(Instant from, Instant to) {
        if (from.isAfter(to)) {
            throw BusinessException.badRequest("RELATORIO_PERIODO_INVALIDO", "'from' deve ser anterior a 'to'.");
        }
        long months = ChronoUnit.MONTHS.between(
                from.atZone(BRT).toLocalDate(),
                to.atZone(BRT).toLocalDate());
        if (months > MAX_MONTHS) {
            throw BusinessException.badRequest("RELATORIO_PERIODO_MUITO_LONGO", "Período máximo é 24 meses.");
        }
    }

    private void validateTipo(String tipo) {
        if (!List.of("FUNIL", "PERFORMANCE_ASSESSOR", "TAREFAS_SLA").contains(tipo)) {
            throw BusinessException.badRequest("RELATORIO_TIPO_INVALIDO", "Tipo inválido: " + tipo);
        }
    }

    private Instant[] previousPeriod(Instant from, Instant to) {
        long days = ChronoUnit.DAYS.between(from, to);
        return new Instant[]{from.minus(days, ChronoUnit.DAYS), from};
    }

    private Map<String, Object> findByStatus(List<Map<String, Object>> rows, String status) {
        return rows.stream().filter(r -> status.equals(r.get("status"))).findFirst().orElse(null);
    }

    private long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Long l) return l;
        if (v instanceof Number n) return n.longValue();
        return 0L;
    }

    private double toDouble(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Double d) return d;
        if (v instanceof Number n) return n.doubleValue();
        return 0.0;
    }

    private String fmtPeriod(Instant from, Instant to) {
        LocalDate f = from.atZone(BRT).toLocalDate();
        LocalDate t = to.atZone(BRT).toLocalDate();
        return f + "/" + t;
    }
}
