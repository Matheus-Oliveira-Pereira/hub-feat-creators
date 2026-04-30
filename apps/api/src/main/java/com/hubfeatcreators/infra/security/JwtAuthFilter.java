package com.hubfeatcreators.infra.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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
          UUID assessoriaId = UUID.fromString((String) claims.get("ass"));
          String role = (String) claims.get("role");

          var authorities = new ArrayList<SimpleGrantedAuthority>();
          authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

          Authentication auth =
              new UsernamePasswordAuthenticationToken(assessoriaId, null, authorities);
          SecurityContextHolder.getContext().setAuthentication(auth);
        }
      }
    } catch (Exception e) {
      logger.debug("JWT validation failed", e);
    }

    filterChain.doFilter(request, response);
  }
}
