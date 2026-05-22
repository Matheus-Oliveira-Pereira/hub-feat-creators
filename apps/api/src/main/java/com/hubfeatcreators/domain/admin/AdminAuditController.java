package com.hubfeatcreators.domain.admin;

import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/audit")
@RequirePermission(PermissionCodes.ADMN)
public class AdminAuditController {

    private final AdminAuditLogRepository repo;

    public AdminAuditController(AdminAuditLogRepository repo) {
        this.repo = repo;
    }

    record AuditEntry(
            UUID id,
            UUID adminId,
            String acao,
            UUID assessoriaIdAfetada,
            String recurso,
            UUID recursoId,
            String ip,
            Instant createdAt) {}

    record PagedResponse(List<AuditEntry> items, long total, int page, int size) {}

    @GetMapping
    public PagedResponse list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) UUID adminId,
            @RequestParam(required = false) String acao,
            @RequestParam(required = false) UUID assessoriaId) {

        int safeSize = Math.min(size, 200);
        PageRequest pr = PageRequest.of(page, safeSize, Sort.by("createdAt").descending());

        Page<AdminAuditLog> result;
        if (adminId != null) {
            result = repo.findByAdminId(adminId, pr);
        } else if (acao != null) {
            result = repo.findByAcao(acao, pr);
        } else if (assessoriaId != null) {
            result = repo.findByAssessoriaIdAfetada(assessoriaId, pr);
        } else {
            result = repo.findAllPaged(pr);
        }

        List<AuditEntry> items =
                result.getContent().stream()
                        .map(
                                a ->
                                        new AuditEntry(
                                                a.getId(),
                                                a.getAdminId(),
                                                a.getAcao(),
                                                a.getAssessoriaIdAfetada(),
                                                a.getRecurso(),
                                                a.getRecursoId(),
                                                a.getIp(),
                                                a.getCreatedAt()))
                        .toList();
        return new PagedResponse(items, result.getTotalElements(), page, safeSize);
    }
}
