package com.hubfeatcreators.relatorio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.hubfeatcreators.domain.relatorio.RelatorioSalvo;
import com.hubfeatcreators.domain.relatorio.RelatorioSalvoRepository;
import com.hubfeatcreators.domain.relatorio.RelatorioService;
import com.hubfeatcreators.domain.relatorio.RelatorioService.*;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.web.BusinessException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RelatorioServiceTest {

    @Mock JdbcTemplate jdbc;
    @Mock RelatorioSalvoRepository savedRepo;

    RelatorioService service;

    UUID assessoriaId = UUID.randomUUID();
    UUID usuarioId = UUID.randomUUID();
    AuthPrincipal principal;

    Instant from = Instant.parse("2025-01-01T00:00:00Z");
    Instant to = Instant.parse("2025-01-31T00:00:00Z");

    @BeforeEach
    void setUp() {
        service = new RelatorioService(jdbc, savedRepo);
        principal = new AuthPrincipal(usuarioId, assessoriaId, "OWNER", Set.of("OWNR"));
    }

    // ─── validate ────────────────────────────────────────────────────────────

    @Test
    void validate_fromAfterTo_throws() {
        stubFunilQuery(List.of());
        assertThatThrownBy(() -> service.funil(assessoriaId, to, from, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void validate_periodExceeds24Months_throws() {
        Instant longFrom = Instant.parse("2022-01-01T00:00:00Z");
        Instant longTo = Instant.parse("2024-03-01T00:00:00Z");
        assertThatThrownBy(() -> service.funil(assessoriaId, longFrom, longTo, null))
                .isInstanceOf(BusinessException.class);
    }

    // ─── funil ───────────────────────────────────────────────────────────────

    @Test
    void funil_emptyData_returnsAllStatusesWithZeros() {
        stubFunilQuery(List.of());
        FunilResult result = service.funil(assessoriaId, from, to, null);
        assertThat(result.itens()).hasSize(5);
        assertThat(result.itens()).allSatisfy(i -> {
            assertThat(i.qtd()).isZero();
            assertThat(i.valorTotalCentavos()).isZero();
        });
    }

    @Test
    void funil_withData_computesDelta() {
        stubFunilQuery(List.of(
                Map.of("status", "NOVA", "qtd", 10L, "valor_total_centavos", 0L, "tempo_medio_dias", 0.0),
                Map.of("status", "NOVA", "qtd", 5L, "valor_total_centavos", 0L, "tempo_medio_dias", 0.0)
        ));
        FunilResult result = service.funil(assessoriaId, from, to, null);
        FunilItem nova = result.itens().stream().filter(i -> "NOVA".equals(i.status())).findFirst().orElseThrow();
        assertThat(nova.qtd()).isEqualTo(10L);
        assertThat(nova.qtdAnterior()).isEqualTo(10L);
        // current == anterior → delta == 0
        assertThat(nova.deltaPercent()).isZero();
    }

    @Test
    void funil_withFilter_passesAssessorId() {
        UUID assessorId = UUID.randomUUID();
        stubFunilQuery(List.of());
        service.funil(assessoriaId, from, to, assessorId);
        verify(jdbc, atLeastOnce()).queryForList(anyString(),
                eq(assessoriaId), any(Instant.class), any(Instant.class),
                eq(assessorId), eq(assessorId));
    }

    @Test
    void funil_noData_currentGt0_delta100() {
        when(jdbc.queryForList(anyString(), eq(assessoriaId), any(Instant.class), any(Instant.class), isNull(), isNull()))
                .thenReturn(List.of(
                        Map.of("status", "NOVA", "qtd", 5L, "valor_total_centavos", 0L, "tempo_medio_dias", 0.0)))
                .thenReturn(List.of());
        FunilResult result = service.funil(assessoriaId, from, to, null);
        FunilItem nova = result.itens().stream().filter(i -> "NOVA".equals(i.status())).findFirst().orElseThrow();
        assertThat(nova.deltaPercent()).isEqualTo(100.0);
    }

    // ─── performanceAssessor ─────────────────────────────────────────────────

    @Test
    void performance_empty_returnsEmptyList() {
        // 3-arg varargs (assessoriaId, from, to) → performance queries
        when(jdbc.queryForList(anyString(), any(UUID.class), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());
        // 1-arg varargs (assessoriaId) → usuarios query
        when(jdbc.queryForList(anyString(), eq(assessoriaId)))
                .thenReturn(List.of());
        PerformanceResult result = service.performanceAssessor(assessoriaId, from, to);
        assertThat(result.assessores()).isEmpty();
    }

    @Test
    void performance_withRow_mapsEmail() {
        UUID aid = UUID.randomUUID();
        when(jdbc.queryForList(anyString(), any(UUID.class), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(
                        Map.of("assessor_id", aid, "abertas", 3L, "ganhas", 1L, "perdidas", 0L,
                                "ticket_medio_centavos", 5000L, "tempo_medio_fechamento_dias", 2.5)))
                .thenReturn(List.of());
        when(jdbc.queryForList(anyString(), eq(assessoriaId)))
                .thenReturn(List.of(Map.of("id", aid, "email", "assessor@test.com")));
        PerformanceResult result = service.performanceAssessor(assessoriaId, from, to);
        assertThat(result.assessores()).hasSize(1);
        assertThat(result.assessores().get(0).assessorEmail()).isEqualTo("assessor@test.com");
        assertThat(result.assessores().get(0).abertas()).isEqualTo(3L);
    }

    // ─── tarefaSla ───────────────────────────────────────────────────────────

    @Test
    void sla_empty_returnsEmptyList() {
        // 5-arg varargs (assessoriaId, from, to, null, null) → SLA query
        when(jdbc.queryForList(anyString(), eq(assessoriaId), any(Instant.class), any(Instant.class), isNull(), isNull()))
                .thenReturn(List.of());
        when(jdbc.queryForList(anyString(), eq(assessoriaId)))
                .thenReturn(List.of());
        TarefaSlaResult result = service.tarefaSla(assessoriaId, from, to, null);
        assertThat(result.assessores()).isEmpty();
    }

    @Test
    void sla_withRow_computesPct() {
        UUID rid = UUID.randomUUID();
        when(jdbc.queryForList(anyString(), eq(assessoriaId), any(Instant.class), any(Instant.class), isNull(), isNull()))
                .thenReturn(List.of(
                        Map.of("responsavel_id", rid, "total", 10L, "no_prazo", 8L,
                                "atrasadas", 2L, "atraso_medio_horas", 3.5)))
                .thenReturn(List.of());
        when(jdbc.queryForList(anyString(), eq(assessoriaId)))
                .thenReturn(List.of(Map.of("id", rid, "email", "resp@test.com")));
        TarefaSlaResult result = service.tarefaSla(assessoriaId, from, to, null);
        assertThat(result.assessores()).hasSize(1);
        assertThat(result.assessores().get(0).total()).isEqualTo(10L);
        assertThat(result.assessores().get(0).noPrazo()).isEqualTo(8L);
    }

    // ─── Relatórios Salvos ────────────────────────────────────────────────────

    @Test
    void salvar_validTipo_persists() {
        RelatorioSalvo saved = new RelatorioSalvo(assessoriaId, usuarioId, "Funil Jan", "FUNIL", Map.of());
        when(savedRepo.save(any())).thenReturn(saved);
        RelatorioSalvo result = service.salvar(principal, "Funil Jan", "FUNIL", Map.of());
        assertThat(result.getNome()).isEqualTo("Funil Jan");
        verify(savedRepo).save(any());
    }

    @Test
    void salvar_invalidTipo_throws() {
        assertThatThrownBy(() -> service.salvar(principal, "X", "INVALIDO", Map.of()))
                .isInstanceOf(BusinessException.class);
        verify(savedRepo, never()).save(any());
    }

    @Test
    void listar_delegatesToRepo() {
        when(savedRepo.findByAssessoriaIdOrderByCreatedAtDesc(assessoriaId)).thenReturn(List.of());
        List<RelatorioSalvo> result = service.listarSalvos(principal);
        assertThat(result).isEmpty();
        verify(savedRepo).findByAssessoriaIdOrderByCreatedAtDesc(assessoriaId);
    }

    @Test
    void deletar_found_deletes() {
        RelatorioSalvo saved = new RelatorioSalvo(assessoriaId, usuarioId, "X", "FUNIL", Map.of());
        UUID id = saved.getId();
        when(savedRepo.findByIdAndAssessoriaId(id, assessoriaId)).thenReturn(Optional.of(saved));
        service.deletarSalvo(principal, id);
        verify(savedRepo).delete(saved);
    }

    @Test
    void deletar_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(savedRepo.findByIdAndAssessoriaId(id, assessoriaId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deletarSalvo(principal, id))
                .isInstanceOf(BusinessException.class);
    }

    // ─── CSV export ───────────────────────────────────────────────────────────

    @Test
    void exportFunilCsv_writesHeaderAndRows() {
        stubFunilQuery(List.of());
        StringWriter sw = new StringWriter();
        service.exportFunilCsv(assessoriaId, from, to, null, new PrintWriter(sw));
        String csv = sw.toString();
        assertThat(csv).contains("status,qtd");
        assertThat(csv).contains("NOVA");
    }

    // ─── previousPeriod ──────────────────────────────────────────────────────

    @Test
    void funil_periodLabel_containsDates() {
        // from/to are midnight UTC; BRT=UTC-3 shifts dates back one day in labels
        stubFunilQuery(List.of());
        FunilResult result = service.funil(assessoriaId, from, to, null);
        assertThat(result.periodoAtual()).contains("2024-12-31"); // 2025-01-01T00Z → BRT 2024-12-31
        assertThat(result.periodoAnterior()).contains("2024-12");
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void stubFunilQuery(List<Map<String, Object>> rows) {
        when(jdbc.queryForList(anyString(), eq(assessoriaId), any(Instant.class), any(Instant.class),
                isNull(), isNull()))
                .thenReturn(rows);
    }
}
