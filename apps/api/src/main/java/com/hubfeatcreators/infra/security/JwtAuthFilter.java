package com.hubfeatcreators.infra.security;

import com.hubfeatcreators.infra.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthFilter extends OncePerRequestFilter {
  private final JwtService jwtService;

  public JwtAuthFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String authHeader = request.getHeader("Authorization");
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        if (jwtService.isTokenValid(token)) {
          Claims claims = jwtService.parseToken(token);
          UUID usuarioId = UUID.fromString(claims.getSubject());
          UUID assessoriaId = UUID.fromString(claims.get("ass", String.class));
          String role = claims.get("role", String.class);
          Set<String> permissions = readPerms(claims);

          AuthPrincipal principal =
              new AuthPrincipal(usuarioId, assessoriaId, role, permissions);
          TenantContext.setAssessoriaId(assessoriaId);

          var auth =
              new UsernamePasswordAuthenticationToken(
                  principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      }
    } catch (Exception e) {
      logger.debug("JWT validation failed", e);
    }

    try {
      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }

  @SuppressWarnings("unchecked")
  private Set<String> readPerms(Claims claims) {
    Object raw = claims.get("perms");
    if (raw instanceof List<?> list) {
      Set<String> out = new LinkedHashSet<>();
      for (Object o : list) {
        if (o != null) out.add(o.toString());
      }
      return out;
    }
    return Set.of();
  }
}
