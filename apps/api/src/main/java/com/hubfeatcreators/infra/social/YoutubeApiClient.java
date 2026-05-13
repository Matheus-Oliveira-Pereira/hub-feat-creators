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
public class YoutubeApiClient {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String API_BASE = "https://www.googleapis.com/youtube/v3";

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public YoutubeApiClient(
            ObjectMapper mapper,
            @Value("${app.social.youtube.client-id:}") String clientId,
            @Value("${app.social.youtube.client-secret:}") String clientSecret,
            @Value(
                            "${app.social.youtube.redirect-uri:http://localhost:8080/api/v1/social/auth/YOUTUBE/callback}")
                    String redirectUri) {
        this.mapper = mapper;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public String buildAuthUrl(String state) {
        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id="
                + clientId
                + "&redirect_uri="
                + redirectUri
                + "&scope=https://www.googleapis.com/auth/youtube.readonly"
                + "&response_type=code"
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state="
                + state;
    }

    public record TokenResponse(String accessToken, String refreshToken, Long expiresIn) {}

    public TokenResponse exchangeCode(String code) {
        try {
            String body =
                    "client_id="
                            + clientId
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
                throw new SocialApiException("YT token exchange failed", res.statusCode());
            JsonNode node = mapper.readTree(res.body());
            return new TokenResponse(
                    node.get("access_token").asText(),
                    node.has("refresh_token") ? node.get("refresh_token").asText() : null,
                    node.get("expires_in").asLong());
        } catch (SocialApiException e) {
            throw e;
        } catch (Exception e) {
            throw new SocialApiException("YT token exchange error", 0, e);
        }
    }

    public TokenResponse refreshToken(String refreshToken) {
        try {
            String body =
                    "client_id="
                            + clientId
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
                throw new SocialApiException("YT token refresh failed", res.statusCode());
            JsonNode node = mapper.readTree(res.body());
            return new TokenResponse(
                    node.get("access_token").asText(),
                    refreshToken,
                    node.get("expires_in").asLong());
        } catch (SocialApiException e) {
            throw e;
        } catch (Exception e) {
            throw new SocialApiException("YT token refresh error", 0, e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getChannelStats(String accessToken) {
        try {
            HttpRequest req =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            API_BASE
                                                    + "/channels?part=snippet,statistics&mine=true"))
                            .header("Authorization", "Bearer " + accessToken)
                            .GET()
                            .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200)
                throw new SocialApiException("YT channel stats failed", res.statusCode());
            return mapper.readValue(res.body(), Map.class);
        } catch (SocialApiException e) {
            throw e;
        } catch (Exception e) {
            throw new SocialApiException("YT channel stats error", 0, e);
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getRecentVideos(String accessToken, String channelId) {
        try {
            HttpRequest req =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            API_BASE
                                                    + "/search?part=snippet&channelId="
                                                    + channelId
                                                    + "&type=video&order=date&maxResults=30"))
                            .header("Authorization", "Bearer " + accessToken)
                            .GET()
                            .build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() != 200)
                throw new SocialApiException("YT videos fetch failed", res.statusCode());
            return mapper.readValue(res.body(), Map.class);
        } catch (SocialApiException e) {
            throw e;
        } catch (Exception e) {
            throw new SocialApiException("YT videos fetch error", 0, e);
        }
    }
}
