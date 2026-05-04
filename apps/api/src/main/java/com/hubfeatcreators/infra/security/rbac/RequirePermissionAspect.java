package com.hubfeatcreators.infra.security.rbac;

import com.hubfeatcreators.infra.security.AuthPrincipal;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Verifica {@link RequirePermission} em handlers de controller. Roda fora do escopo do {@code
 * TenantAspect} (Order 1 vs 3 do tenant) — não precisa estar dentro de transação.
 */
@Aspect
@Component
@Order(1)
public class RequirePermissionAspect {

    private final MeterRegistry meterRegistry;

    public RequirePermissionAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around(
            "@annotation(com.hubfeatcreators.infra.security.rbac.RequirePermission) "
                    + "|| @within(com.hubfeatcreators.infra.security.rbac.RequirePermission)")
    public Object check(ProceedingJoinPoint pjp) throws Throwable {
        RequirePermission anno = resolveAnnotation(pjp);
        if (anno == null) return pjp.proceed();

        AuthPrincipal principal = currentPrincipal();
        if (principal == null) {
            recordDenied("anonymous", anno);
            throw new AccessDeniedException("Autenticação requerida.");
        }

        Set<String> required = Set.of(anno.value());
        boolean ok =
                anno.mode() == RequirePermission.Mode.ALL_OF
                        ? principal.hasAllPermissions(required)
                        : principal.hasAnyPermission(required);

        if (!ok) {
            recordDenied(String.join(",", required), anno);
            throw new AccessDeniedException(
                    "Permissão insuficiente. Requer: " + String.join(", ", required));
        }
        return pjp.proceed();
    }

    private RequirePermission resolveAnnotation(ProceedingJoinPoint pjp) {
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        RequirePermission method = sig.getMethod().getAnnotation(RequirePermission.class);
        if (method != null) return method;
        return sig.getMethod().getDeclaringClass().getAnnotation(RequirePermission.class);
    }

    private AuthPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal p)) return null;
        return p;
    }

    private void recordDenied(String requested, RequirePermission anno) {
        Counter.builder("auth_permission_denied_total")
                .description("Acessos negados por aspect @RequirePermission")
                .tag("required", requested)
                .tag("mode", anno.mode().name())
                .register(meterRegistry)
                .increment();
    }
}
