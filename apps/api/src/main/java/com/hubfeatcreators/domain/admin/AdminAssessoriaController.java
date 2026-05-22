package com.hubfeatcreators.domain.admin;

import com.hubfeatcreators.domain.assessoria.Assessoria;
import com.hubfeatcreators.domain.assessoria.AssessoriaRepository;
import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.domain.usuario.UsuarioRepository;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import com.hubfeatcreators.infra.web.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/assessorias")
@RequirePermission(PermissionCodes.ADMN)
public class AdminAssessoriaController {

    private final AssessoriaRepository assessoriaRepo;
    private final UsuarioRepository usuarioRepo;
    private final AdminAuditService auditService;

    public AdminAssessoriaController(
            AssessoriaRepository assessoriaRepo,
            UsuarioRepository usuarioRepo,
            AdminAuditService auditService) {
        this.assessoriaRepo = assessoriaRepo;
        this.usuarioRepo = usuarioRepo;
        this.auditService = auditService;
    }

    record AssessoriaItem(UUID id, String nome, String slug, String plano, Instant createdAt) {}

    record AssessoriaDetail(
            UUID id,
            String nome,
            String slug,
            String plano,
            Instant createdAt,
            long totalUsuarios) {}

    record PagedResponse(List<AssessoriaItem> items, long total, int page, int size) {}

    record EditRequest(String nome, String plano) {}

    @GetMapping
    public PagedResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, 100);
        Page<Assessoria> result =
                assessoriaRepo.findAllActive(
                        PageRequest.of(page, safeSize, Sort.by("createdAt").descending()));
        List<AssessoriaItem> items =
                result.getContent().stream()
                        .map(
                                a ->
                                        new AssessoriaItem(
                                                a.getId(),
                                                a.getNome(),
                                                a.getSlug(),
                                                a.getPlano(),
                                                a.getCreatedAt()))
                        .toList();
        return new PagedResponse(items, result.getTotalElements(), page, safeSize);
    }

    @GetMapping("/{id}")
    public AssessoriaDetail get(@PathVariable UUID id) {
        Assessoria a =
                assessoriaRepo
                        .findById(id)
                        .filter(x -> x.getDeletedAt() == null)
                        .orElseThrow(() -> BusinessException.notFound("ASSESSORIA"));
        long usuarioCount = usuarioRepo.findAllByAssessoriaId(a.getId()).size();
        return new AssessoriaDetail(
                a.getId(), a.getNome(), a.getSlug(), a.getPlano(), a.getCreatedAt(), usuarioCount);
    }

    @PutMapping("/{id}")
    public AssessoriaItem edit(
            @PathVariable UUID id,
            @Valid @RequestBody EditRequest req,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest http) {
        Assessoria a =
                assessoriaRepo
                        .findById(id)
                        .filter(x -> x.getDeletedAt() == null)
                        .orElseThrow(() -> BusinessException.notFound("ASSESSORIA"));

        String before = "{\"nome\":\"" + a.getNome() + "\",\"plano\":\"" + a.getPlano() + "\"}";
        if (req.nome() != null) a.setNome(req.nome());
        if (req.plano() != null) a.setPlano(req.plano());
        a.setUpdatedAt(Instant.now());
        assessoriaRepo.save(a);

        auditService.log(
                principal.usuarioId(),
                "ASSESSORIA_EDIT",
                id,
                "assessoria",
                id,
                before,
                null,
                ip(http),
                http.getHeader("User-Agent"));
        return new AssessoriaItem(
                a.getId(), a.getNome(), a.getSlug(), a.getPlano(), a.getCreatedAt());
    }

    @PostMapping("/{id}/anonymize")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void anonymize(
            @PathVariable UUID id,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest http) {
        Assessoria a =
                assessoriaRepo
                        .findById(id)
                        .filter(x -> x.getDeletedAt() == null)
                        .orElseThrow(() -> BusinessException.notFound("ASSESSORIA"));

        String before = "{\"nome\":\"" + a.getNome() + "\"}";
        a.setNome("ANONYMIZED-" + id);
        a.setDeletedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
        assessoriaRepo.save(a);

        auditService.log(
                principal.usuarioId(),
                "ASSESSORIA_EDIT",
                id,
                "assessoria_anonymize",
                id,
                before,
                null,
                ip(http),
                http.getHeader("User-Agent"));
    }

    private String ip(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        return xff != null ? xff.split(",")[0].trim() : req.getRemoteAddr();
    }
}
