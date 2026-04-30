package com.hubfeatcreators.infra.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Runs inside every @Transactional boundary (order=3, @Transactional is order=2).
 * Sets the Hibernate tenant_filter and Postgres SET LOCAL so RLS picks it up.
 */
@Aspect
@Component
@Order(3)
public class TenantAspect {

  @PersistenceContext private EntityManager em;

  @Before(
      "@within(org.springframework.transaction.annotation.Transactional)"
          + " || @annotation(org.springframework.transaction.annotation.Transactional)")
  public void applyTenant() {
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
              conn.prepareStatement("SELECT set_config('app.assessoria_id', ?, true)")) {
            stmt.setString(1, id);
            stmt.execute();
          }
        });
  }
}
