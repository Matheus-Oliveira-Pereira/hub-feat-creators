package com.hubfeatcreators.infra.tenant;

import com.hubfeatcreators.domain.admin.AdminAuditService;
import com.hubfeatcreators.infra.security.AuthPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Runs inside every @Transactional boundary (order=3, @Transactional is order=2). Sets the
 * Hibernate tenant_filter and Postgres SET LOCAL so RLS picks it up. ADM users bypass tenant check
 * — every bypass is logged in admin_audit_log.
 */
@Aspect
@Component
@Order(3)
public class TenantAspect {

    @PersistenceContext private EntityManager em;

    private final AdminAuditService adminAuditService;

    public TenantAspect(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @Before(
            "@within(org.springframework.transaction.annotation.Transactional)"
                    + " || @annotation(org.springframework.transaction.annotation.Transactional)")
    public void applyTenant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.getPrincipal() instanceof AuthPrincipal principal
                && principal.isAdm()) {
            // ADM bypasses tenant isolation — log each bypass
            UUID assessoriaId = TenantContext.getAssessoriaId();
            String path = currentPath();
            String method = currentMethod();
            adminAuditService.logTenantBypass(
                    principal.usuarioId(), path, assessoriaId, method, currentIp(), currentUa());
            return;
        }

        UUID assessoriaId = TenantContext.getAssessoriaId();
        if (assessoriaId == null) return;

        Session session = em.unwrap(Session.class);

        Filter filter = session.enableFilter("tenant_filter");
        filter.setParameter("assessoriaId", assessoriaId);

        // SET LOCAL scopes to current transaction — safe with connection pooling
        final String id = assessoriaId.toString();
        session.doWork(
                conn -> {
                    try (var stmt =
                            conn.prepareStatement(
                                    "SELECT set_config('app.assessoria_id', ?, true)")) {
                        stmt.setString(1, id);
                        stmt.execute();
                    }
                });
    }

    private String currentPath() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest().getRequestURI() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String currentMethod() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest().getMethod() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String currentIp() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            return xff != null ? xff.split(",")[0].trim() : req.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }

    private String currentUa() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest().getHeader("User-Agent") : null;
        } catch (Exception e) {
            return null;
        }
    }
}
