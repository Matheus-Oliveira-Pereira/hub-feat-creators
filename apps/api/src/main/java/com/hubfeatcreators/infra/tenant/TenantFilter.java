package com.hubfeatcreators.infra.tenant;

import com.hubfeatcreators.infra.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class TenantFilter extends OncePerRequestFilter {
  private final org.hibernate.SessionFactory sessionFactory;

  public TenantFilter(org.hibernate.SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    try {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      UUID assessoriaId = null;

      if (auth != null && auth.isAuthenticated()) {
        assessoriaId = (UUID) auth.getPrincipal();
      }

      if (assessoriaId != null) {
        TenantContext.setAssessoriaId(assessoriaId);
        Session session = sessionFactory.getCurrentSession();
        Filter filter = session.enableFilter("tenant_filter");
        filter.setParameter("assessoriaId", assessoriaId);
      }

      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }
}
