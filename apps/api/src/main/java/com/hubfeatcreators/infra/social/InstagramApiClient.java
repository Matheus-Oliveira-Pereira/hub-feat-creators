package com.hubfeatcreators.infra.social;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InstagramApiClient {

    private static final String BASE = "https://graph.instagram.com/v18.0";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper;
    private final String appId;
    private final String appSecret;
    private final String redirectUri;

    public InstagramApiClient(
            ObjectMapper mapper,
            @Value("${app.social.instagram.app-id:}") String appId,
            @Value("${app.social.instagram.app-secret:}") String appSecret,
            @Value(
                            "${app.social.instagram.redirect-uri:http://localhost:8080/api/v1/social/auth/INSTAGRAM/callback}")
                    String redirectUri) {
        this.mapper = mapper;
        this.appId = appId;
        this.appSecret = appSecret;
        this.redirectUri = redirectUri;
    }

    public String buildAuthUrl(String state) {
        return "https://api.instagram.com/oauth/authorize"
                + "?client_id="
                + appId
                + "&redirect_uri="
                + redirectUri
                + "&scope=instagram_basic,instagram_manage_insights"
                + "&response_type=code"
                + "&state="
                + state;
    }

    public record TokenResponse(String accessToken, String userId, Long expiresIn) {}

    public TokenResponse exchangeCode(String code) {
        try {
            String body =
                    "client_id="
                            + appId
                            + "&client_secret="
                            + appSecret
                            + "&grant_type=authorization_code"
                            + "&redirect_uri="
                            + redirectUri
                            + "&code="
                            + code;
            HttpRequest req =
                    HttpRequest.newBuilder()
                            .uri(URI.create("https://api.instagram.com/oauth/access_token"))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200)
                throw new SocialApiException("IG token exchange failed", res.statusCode());
            JsonNode node = mapper.readTree(res.body());
            return new TokenResponse(
                    node.get("access_token").asText(),
                    node.get("user_id").asText(),
                    node.has("expires_in") ? node.get("expires_in").asLong() : 5183944L);
        } catch (SocialApiException e) {
            throw e;
        } catch (Exception e) {
            throw new SocialApiException("IG token exchange error", 0, e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserProfile(String accessToken) {
        try {
            HttpRequest req =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            BASE
                                                    + "/me?fields=id,username,followers_count,follows_count,media_count&access_token="
                                                    + accessToken))
                            .GET()
                            .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200)
                throw new SocialApiException("IG profile fetch failed", res.statusCode());
            return mapper.readValue(res.body(), Map.class);
        } catch (SocialApiException e) {
            throw e;
        } catch (Exception e) {
            throw new SocialApiException("IG profile fetch error", 0, e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getRecentMedia(String accessToken) {
        try {
            HttpRequest req =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            BASE
                                                    + "/me/media?fields=id,caption,media_type,timestamp,like_count,comments_count,permalink&limit=30&access_token="
                                                    + accessToken))
                            .GET()
                            .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200)
                throw new SocialApiException("IG media fetch failed", res.statusCode());
            return mapper.readValue(res.body(), Map.class);
        } catch (SocialApiException e) {
            throw e;
        } catch (Exception e) {
            throw new SocialApiException("IG media fetch error", 0, e);
        }
    }
}
