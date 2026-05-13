package com.hubfeatcreators.domain.relatorio;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class RelatorioController {

    private final RelatorioService service;

    public RelatorioController(RelatorioService service) {
        this.service = service;
    }

    // ─── Reports ─────────────────────────────────────────────────────────────

    @GetMapping("/relatorios/funil")
    @RequirePermission(PermissionCodes.B_REL)
    public RelatorioService.FunilResult funil(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false) UUID assessorId) {
        return service.funil(principal.assessoriaId(), from, to, assessorId);
    }

    @GetMapping("/relatorios/performance-assessor")
    @RequirePermission(PermissionCodes.B_REL)
    public RelatorioService.PerformanceResult performanceAssessor(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam Instant from,
            @RequestParam Instant to) {
        return service.performanceAssessor(principal.assessoriaId(), from, to);
    }

    @GetMapping("/relatorios/tarefas-sla")
    @RequirePermission(PermissionCodes.B_REL)
    public RelatorioService.TarefaSlaResult tarefasSla(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false) UUID responsavelId) {
        return service.tarefaSla(principal.assessoriaId(), from, to, responsavelId);
    }

    // ─── CSV Export ───────────────────────────────────────────────────────────

    @GetMapping("/relatorios/funil/export.csv")
    @RequirePermission(PermissionCodes.EXPT)
    public void exportFunil(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false) UUID assessorId,
            HttpServletResponse response)
            throws Exception {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"funil.csv\"");
        try (PrintWriter out = response.getWriter()) {
            service.exportFunilCsv(principal.assessoriaId(), from, to, assessorId, out);
        }
    }

    @GetMapping("/relatorios/performance-assessor/export.csv")
    @RequirePermission(PermissionCodes.EXPT)
    public void exportPerformance(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam Instant from,
            @RequestParam Instant to,
            HttpServletResponse response)
            throws Exception {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"performance-assessor.csv\"");
        try (PrintWriter out = response.getWriter()) {
            service.exportPerformanceCsv(principal.assessoriaId(), from, to, out);
        }
    }

    @GetMapping("/relatorios/tarefas-sla/export.csv")
    @RequirePermission(PermissionCodes.EXPT)
    public void exportSla(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(required = false) UUID responsavelId,
            HttpServletResponse response)
            throws Exception {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tarefas-sla.csv\"");
        try (PrintWriter out = response.getWriter()) {
            service.exportSlavCsv(principal.assessoriaId(), from, to, responsavelId, out);
        }
    }

    // ─── Relatórios Salvos ────────────────────────────────────────────────────

    @GetMapping("/relatorios-salvos")
    @RequirePermission(PermissionCodes.B_REL)
    public List<RelatorioSalvo> listar(@AuthenticationPrincipal AuthPrincipal principal) {
        return service.listarSalvos(principal);
    }

    @PostMapping("/relatorios-salvos")
    @RequirePermission(PermissionCodes.B_REL)
    public RelatorioSalvo salvar(
            @AuthenticationPrincipal AuthPrincipal principal, @RequestBody SalvarRequest body) {
        return service.salvar(principal, body.nome(), body.tipo(), body.filtros());
    }

    @DeleteMapping("/relatorios-salvos/{id}")
    @RequirePermission(PermissionCodes.B_REL)
    public void deletar(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable UUID id) {
        service.deletarSalvo(principal, id);
    }

    public record SalvarRequest(String nome, String tipo, Map<String, Object> filtros) {}
}
