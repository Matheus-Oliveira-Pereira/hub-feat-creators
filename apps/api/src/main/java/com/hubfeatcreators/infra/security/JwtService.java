package com.hubfeatcreators.infra.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;
  private final long accessTokenExpiration;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenExpiration = expirationMinutes * 60 * 1000;
  }

  public String generateAccessToken(UUID usuarioId, UUID assessoriaId, String role) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

    return Jwts.builder()
        .setSubject(usuarioId.toString())
        .claim("ass", assessoriaId.toString())
        .claim("role", role)
        .setIssuedAt(now)
        .setExpiration(expiryDate)
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  public Claims parseToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  public UUID getUsuarioId(String token) {
    Claims claims = parseToken(token);
    return UUID.fromString(claims.getSubject());
  }

  public UUID getAssessoriaId(String token) {
    Claims claims = parseToken(token);
    String assStr = claims.get("ass", String.class);
    return UUID.fromString(assStr);
  }

  public String getRole(String token) {
    Claims claims = parseToken(token);
    return claims.get("role", String.class);
  }

  public boolean isTokenValid(String token) {
    try {
      parseToken(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
