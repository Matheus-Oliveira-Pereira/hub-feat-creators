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
public class TiktokApiClient {

    private static final String TOKEN_URL = "https://open.tiktokapis.com/v2/oauth/token/";
    private static final String API_BASE = "https://open.tiktokapis.com/v2";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper;
    private final String clientKey;
    private final String clientSecret;
    private final String redirectUri;

    public TiktokApiClient(
            ObjectMapper mapper,
            @Value("${app.social.tiktok.client-key:}") String clientKey,
            @Value("${app.social.tiktok.client-secret:}") String clientSecret,
            @Value(
                            "${app.social.tiktok.redirect-uri:http://localhost:8080/api/v1/social/auth/TIKTOK/callback}")
                    String redirectUri) {
        this.mapper = mapper;
        this.clientKey = clientKey;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public String buildAuthUrl(String state) {
        return "https://www.tiktok.com/v2/auth/authorize/"
                + "?client_key="
                + clientKey
                + "&redirect_uri="
                + redirectUri
                + "&scope=user.info.basic,video.list"
                + "&response_type=code"
                + "&state="
                + state;
    }

    public record TokenResponse(
            String accessToken, String refreshToken, Long expiresIn, String openId) {}

    public TokenResponse exchangeCode(String code) {
        try {
            String body =
                    "client_key="
                            + clientKey
                            + "&client_secret="
                            + clientSecret
                            + "&grant_type=authorization_code"
                            + "&redirect_uri="
                            + redirectUri
                            + "&code="
                            + code;
            HttpRequest req =
                    HttpRequest.newBuilder()
                            .uri(URI.create(TOKEN_URL))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200)
                throw new SocialApiException("TikTok token exchange failed", res.statusCode());
            JsonNode node = mapper.readTree(res.body()).path("data");
            return new TokenResponse(
                    node.get("access_token").asText(),
                    node.has("refresh_token") ? node.get("refresh_token").asText() : null,
                    node.get("expires_in").asLong(),
                    node.get("open_id").asText());
        } catch (SocialApiException e) {
            throw e;
        } catch (Exception e) {
            throw new SocialApiException("TikTok token exchange error", 0, e);
        }
    }

    public TokenResponse refreshToken(String refreshToken) {
        try {
            String body =
                    "client_key="
                            + clientKey
                            + "&client_secret="
                            + clientSecret
                            + "&grant_type=refresh_token"
                            + "&refresh_token="
                            + refreshToken;
            HttpRequest req =
                    HttpRequest.newBuilder()
                            .uri(URI.create(TOKEN_URL))
                            .header("Content-Type", "application/x-www-form-urlencoded")
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200)
                throw new SocialApiException("TikTok token refresh failed", res.statusCode());
            JsonNode node = mapper.readTree(res.body()).path("data");
            return new TokenResponse(
                    node.get("access_token").asText(),
                    refreshToken,
                    node.get("expires_in").asLong(),
                    null);
        } catch (SocialApiException e) {
            throw e;
        } catch (Exception e) {
            throw new SocialApiException("TikTok token refresh error", 0, e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getUserInfo(String accessToken) {
        try {
            HttpRequest req =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            API_BASE
                                                    + "/user/info/?fields=open_id,display_name,follower_count,following_count,video_count"))
                            .header("Authorization", "Bearer " + accessToken)
                            .GET()
                            .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200)
                throw new SocialApiException("TikTok user info failed", res.statusCode());
            return mapper.readValue(res.body(), Map.class);
        } catch (SocialApiException e) {
            throw e;
        } catch (Exception e) {
            throw new SocialApiException("TikTok user info error", 0, e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getVideoList(String accessToken) {
        try {
            String body =
                    "{\"fields\":[\"id\",\"title\",\"create_time\",\"like_count\",\"comment_count\",\"share_count\",\"view_count\",\"share_url\"],\"max_count\":30}";
            HttpRequest req =
                    HttpRequest.newBuilder()
                            .uri(URI.create(API_BASE + "/video/list/"))
                            .header("Authorization", "Bearer " + accessToken)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200)
                throw new SocialApiException("TikTok video list failed", res.statusCode());
            return mapper.readValue(res.body(), Map.class);
        } catch (SocialApiException e) {
            throw e;
        } catch (Exception e) {
            throw new SocialApiException("TikTok video list error", 0, e);
        }
    }
}
