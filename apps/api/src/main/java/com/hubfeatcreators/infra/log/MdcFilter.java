package com.hubfeatcreators.infra.log;

import com.hubfeatcreators.infra.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class MdcFilter extends OncePerRequestFilter {
  private final JwtService jwtService;

  public MdcFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    try {
      String requestId = request.getHeader("X-Request-Id");
      if (requestId == null || requestId.isEmpty()) {
        requestId = UUID.randomUUID().toString();
      }
      MDC.put("request_id", requestId);

      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.isAuthenticated()) {
        UUID assessoriaId = (UUID) auth.getPrincipal();
        MDC.put("assessoria_id", assessoriaId.toString());
      }

      String authHeader = request.getHeader("Authorization");
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        try {
          String token = authHeader.substring(7);
          UUID usuarioId = jwtService.getUsuarioId(token);
          MDC.put("usuario_id", usuarioId.toString());
        } catch (Exception e) {
          logger.debug("Could not extract usuario_id from token", e);
        }
      }

      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }
}
