package com.hubfeatcreators.infra.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

    public String generateAccessToken(UUID usuarioId, String role, Collection<String> permissions) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);
        return Jwts.builder()
                .setSubject(usuarioId.toString())
                .claim("role", role)
                .claim("perms", List.copyOf(permissions))
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Generates a short-lived access token for portal creator users. */
    public String generateCreatorToken(UUID creatorUserId, UUID influenciadorId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + 24L * 60 * 60 * 1000); // 24h for creators
        return Jwts.builder()
                .setSubject(creatorUserId.toString())
                .claim("inf", influenciadorId.toString())
                .claim("tipo", "CREATOR")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    public UUID getUsuarioId(String token) {
        return UUID.fromString(parseToken(token).getSubject());
    }

    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    @SuppressWarnings("unchecked")
    public Set<String> getPermissions(String token) {
        Object raw = parseToken(token).get("perms");
        if (raw instanceof List<?> list) {
            Set<String> out = new LinkedHashSet<>();
            for (Object o : list) {
                if (o != null) out.add(o.toString());
            }
            return out;
        }
        return Set.of();
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isCreatorToken(String token) {
        try {
            return "CREATOR".equals(parseToken(token).get("tipo", String.class));
        } catch (Exception e) {
            return false;
        }
    }
}
