package com.hubfeatcreators.domain.importacao;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/imports")
public class ImportController {

    private final ImportService service;

    public ImportController(ImportService service) {
        this.service = service;
    }

    // ---- DTOs ---

    record MapeamentoRequest(
            Map<String, String> mapeamento, String baseLegal, String dedupStrategy) {}

    record TemplateCreateRequest(String nome, String entidade, Map<String, String> mapeamento) {}

    record JobResponse(
            UUID id,
            String entidade,
            String arquivoNome,
            String status,
            Integer totalLinhas,
            int processadas,
            int sucesso,
            int falha,
            Instant iniciadoEm,
            Instant concluidoEm,
            Instant createdAt) {}

    record LinhaResponse(int linha, String status, UUID entidadeId, List<String> erros) {}

    record TemplateResponse(
            UUID id,
            String nome,
            String entidade,
            Map<String, String> mapeamento,
            Instant createdAt) {}

    record DryRunPageResponse(List<LinhaResponse> linhas, long total, boolean hasMore) {}

    record JobListResponse(List<JobResponse> jobs, long total, boolean hasMore) {}

    record ProgressResponse(int processadas, int total, int sucesso, int falha, String status) {}

    // ---- Upload ---

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.C_IMP)
    public JobResponse upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("entidade") String entidade,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return toJobResponse(service.upload(principal, file, entidade));
    }

    // ---- Probe headers + auto-suggest ---

    @GetMapping("/{id}/probe")
    @RequirePermission(PermissionCodes.B_IMP)
    public ImportService.ProbeResponse probe(
            @PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
        return service.probe(principal, id);
    }

    // ---- Set mapeamento ---

    @PutMapping("/{id}/mapeamento")
    @RequirePermission(PermissionCodes.C_IMP)
    public JobResponse setMapeamento(
            @PathVariable UUID id,
            @RequestBody MapeamentoRequest req,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return toJobResponse(
                service.setMapeamento(
                        principal, id, req.mapeamento(), req.baseLegal(), req.dedupStrategy()));
    }

    // ---- Dry-run ---

    @PostMapping("/{id}/dry-run")
    @RequirePermission(PermissionCodes.C_IMP)
    public JobResponse dryRun(
            @PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
        return toJobResponse(service.dryRun(principal, id));
    }

    @GetMapping("/{id}/dry-run")
    @RequirePermission(PermissionCodes.B_IMP)
    public DryRunPageResponse listDryRun(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size,
            @AuthenticationPrincipal AuthPrincipal principal) {
        Page<ImportJobLinha> linhas = service.listLinhas(principal, id, page, Math.min(size, 1000));
        List<LinhaResponse> data = linhas.getContent().stream().map(this::toLinhaResponse).toList();
        return new DryRunPageResponse(data, linhas.getTotalElements(), linhas.hasNext());
    }

    // ---- Execute ---

    @PostMapping("/{id}/execute")
    @RequirePermission(PermissionCodes.C_IMP)
    public JobResponse execute(
            @PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
        return toJobResponse(service.execute(principal, id));
    }

    // ---- Cancel ---

    @DeleteMapping("/{id}")
    @RequirePermission(PermissionCodes.C_IMP)
    public JobResponse cancel(
            @PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
        return toJobResponse(service.cancel(principal, id));
    }

    // ---- Status & list ---

    @GetMapping("/{id}")
    @RequirePermission(PermissionCodes.B_IMP)
    public JobResponse get(
            @PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {
        return toJobResponse(service.get(principal, id));
    }

    @GetMapping
    @RequirePermission(PermissionCodes.B_IMP)
    public JobListResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthPrincipal principal) {
        Page<ImportJob> p = service.list(principal, page, Math.min(size, 100));
        return new JobListResponse(
                p.getContent().stream().map(this::toJobResponse).toList(),
                p.getTotalElements(),
                p.hasNext());
    }

    // ---- Relatório CSV ---

    @GetMapping("/{id}/relatorio")
    @RequirePermission(PermissionCodes.B_IMP)
    public void relatorio(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletResponse response)
            throws Exception {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"import-" + id + ".csv\"");
        try (PrintWriter writer = response.getWriter()) {
            service.streamRelatorio(principal, id, writer);
        }
    }

    // ---- Progress SSE ---

    @GetMapping(value = "/{id}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequirePermission(PermissionCodes.B_IMP)
    public SseEmitter progress(
            @PathVariable UUID id, @AuthenticationPrincipal AuthPrincipal principal) {

        SseEmitter emitter = new SseEmitter(120_000L);
        Thread.ofVirtual()
                .start(
                        () -> {
                            try {
                                for (int i = 0; i < 60; i++) {
                                    ImportJob job = service.get(principal, id);
                                    emitter.send(
                                            new ProgressResponse(
                                                    job.getProcessadas(),
                                                    job.getTotalLinhas() != null
                                                            ? job.getTotalLinhas()
                                                            : 0,
                                                    job.getSucesso(),
                                                    job.getFalha(),
                                                    job.getStatus()));
                                    String s = job.getStatus();
                                    if ("CONCLUIDO".equals(s)
                                            || "CANCELADO".equals(s)
                                            || "FALHOU".equals(s)) break;
                                    Thread.sleep(2000);
                                }
                                emitter.complete();
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        });
        return emitter;
    }

    // ---- Templates ---

    @GetMapping("/templates")
    @RequirePermission(PermissionCodes.B_IMP)
    public List<TemplateResponse> listTemplates(
            @RequestParam(required = false) String entidade,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return service.listTemplates(principal, entidade).stream()
                .map(this::toTemplateResponse)
                .toList();
    }

    @PostMapping("/templates")
    @ResponseStatus(HttpStatus.CREATED)
    @RequirePermission(PermissionCodes.C_IMP)
    public TemplateResponse createTemplate(
            @RequestBody TemplateCreateRequest req,
            @AuthenticationPrincipal AuthPrincipal principal) {
        return toTemplateResponse(
                service.saveTemplate(principal, req.nome(), req.entidade(), req.mapeamento()));
    }

    @DeleteMapping("/templates/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequirePermission(PermissionCodes.C_IMP)
    public void deleteTemplate(
            @PathVariable UUID templateId, @AuthenticationPrincipal AuthPrincipal principal) {
        service.deleteTemplate(principal, templateId);
    }

    // ---- Mappers ---

    private JobResponse toJobResponse(ImportJob j) {
        return new JobResponse(
                j.getId(),
                j.getEntidade(),
                j.getArquivoNome(),
                j.getStatus(),
                j.getTotalLinhas(),
                j.getProcessadas(),
                j.getSucesso(),
                j.getFalha(),
                j.getIniciadoEm(),
                j.getConcluidoEm(),
                j.getCreatedAt());
    }

    private LinhaResponse toLinhaResponse(ImportJobLinha l) {
        return new LinhaResponse(l.getLinha(), l.getStatus(), l.getEntidadeId(), l.getErros());
    }

    private TemplateResponse toTemplateResponse(ImportTemplate t) {
        return new TemplateResponse(
                t.getId(), t.getNome(), t.getEntidade(), t.getMapeamento(), t.getCreatedAt());
    }
}
