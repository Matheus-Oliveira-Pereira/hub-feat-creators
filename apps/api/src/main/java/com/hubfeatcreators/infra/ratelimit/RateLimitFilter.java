package com.hubfeatcreators.infra.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * IP-based rate limiting for public endpoints. Buckets are per-IP per route family and live in
 * memory (sufficient for single-instance Railway MVP; swap to Redis backend when scaling).
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Map<String, Bucket> BUCKETS = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String path = req.getRequestURI();
        RouteLimit limit = classify(path);
        if (limit == null) {
            chain.doFilter(req, res);
            return;
        }
        String ip = extractIp(req);
        String key = limit.name() + ":" + ip;
        Bucket bucket = BUCKETS.computeIfAbsent(key, k -> buildBucket(limit));
        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            res.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            res.setContentType("application/json");
            res.getWriter()
                    .write(
                            "{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"Too many requests. Try again later.\"}");
        }
    }

    private RouteLimit classify(String path) {
        if (path.startsWith("/api/v1/auth/")) return RouteLimit.AUTH;
        if (path.startsWith("/api/v1/dsr/")) return RouteLimit.DSR;
        if (path.equals("/api/v1/whatsapp/webhook")) return RouteLimit.WEBHOOK;
        if (path.equals("/api/v1/email/unsubscribe")) return RouteLimit.UNSUBSCRIBE;
        if (path.startsWith("/api/v1/email/track/")) return RouteLimit.TRACK;
        return null;
    }

    private static Bucket buildBucket(RouteLimit limit) {
        return Bucket.builder()
                .addLimit(
                        Bandwidth.builder()
                                .capacity(limit.capacity)
                                .refillIntervally(limit.capacity, Duration.ofMinutes(1))
                                .build())
                .build();
    }

    private static String extractIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private enum RouteLimit {
        AUTH(20),
        DSR(5),
        WEBHOOK(100),
        UNSUBSCRIBE(10),
        TRACK(30);

        final long capacity;

        RouteLimit(long capacity) {
            this.capacity = capacity;
        }
    }
}
