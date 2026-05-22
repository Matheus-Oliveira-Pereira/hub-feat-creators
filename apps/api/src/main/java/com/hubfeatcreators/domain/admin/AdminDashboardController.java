package com.hubfeatcreators.domain.admin;

import com.hubfeatcreators.domain.assessoria.AssessoriaRepository;
import com.hubfeatcreators.domain.rbac.PermissionCodes;
import com.hubfeatcreators.domain.usuario.Usuario;
import com.hubfeatcreators.domain.usuario.UsuarioRepository;
import com.hubfeatcreators.infra.security.rbac.RequirePermission;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequirePermission(PermissionCodes.ADMN)
public class AdminDashboardController {

    private final AssessoriaRepository assessoriaRepo;
    private final UsuarioRepository usuarioRepo;
    private final AdminAuditLogRepository auditRepo;

    public AdminDashboardController(
            AssessoriaRepository assessoriaRepo,
            UsuarioRepository usuarioRepo,
            AdminAuditLogRepository auditRepo) {
        this.assessoriaRepo = assessoriaRepo;
        this.usuarioRepo = usuarioRepo;
        this.auditRepo = auditRepo;
    }

    record DashboardResponse(
            long totalAssessorias,
            long totalUsuariosInternos,
            long totalAdms,
            long auditActionsLast24h) {}

    @GetMapping
    public DashboardResponse dashboard() {
        long assessorias = assessoriaRepo.count();
        long internos = usuarioRepo.countByTipoAndDeletedAtIsNull(Usuario.TipoUsuario.INTERNO);
        long adms = usuarioRepo.countByTipoAndDeletedAtIsNull(Usuario.TipoUsuario.ADM);
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        long auditCount =
                auditRepo
                        .findAllPaged(org.springframework.data.domain.PageRequest.of(0, 1))
                        .getTotalElements();
        return new DashboardResponse(assessorias, internos, adms, auditCount);
    }
}
