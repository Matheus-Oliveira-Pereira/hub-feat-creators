package com.hubfeatcreators.mobile;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubfeatcreators.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

class DeviceSubscriptionIT extends IntegrationTestBase {

    @Autowired TestRestTemplate rest;

    String tokenA;
    String tokenB;

    record SignupReq(String assessoriaNome, String slug, String email, String senha) {}

    record TokenResp(String accessToken) {}

    record RegisterReq(String canal, String token, String plataforma) {}

    @BeforeEach
    void setup() {
        long ts = System.nanoTime();
        tokenA = signup("mobile-a-" + ts, "mobile.a" + ts + "@test.com");
        tokenB = signup("mobile-b-" + ts, "mobile.b" + ts + "@test.com");
    }

    String signup(String slug, String email) {
        return rest.postForEntity(
                        baseUrl("/api/v1/auth/signup"),
                        new SignupReq("Assessoria " + slug, slug, email, "senha123456"),
                        TokenResp.class)
                .getBody()
                .accessToken();
    }

    HttpHeaders auth(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    @Test
    void register_device_returns_204() {
        var req = new RegisterReq("FCM", "fcm-token-abc123-" + System.nanoTime(), "android");
        ResponseEntity<Void> resp =
                rest.exchange(
                        baseUrl("/api/v1/devices/register"),
                        HttpMethod.POST,
                        new HttpEntity<>(req, auth(tokenA)),
                        Void.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void register_same_token_twice_is_idempotent() {
        String token = "fcm-idempotent-" + System.nanoTime();
        var req = new RegisterReq("FCM", token, "android");

        rest.exchange(
                baseUrl("/api/v1/devices/register"),
                HttpMethod.POST,
                new HttpEntity<>(req, auth(tokenA)),
                Void.class);

        ResponseEntity<Void> second =
                rest.exchange(
                        baseUrl("/api/v1/devices/register"),
                        HttpMethod.POST,
                        new HttpEntity<>(req, auth(tokenA)),
                        Void.class);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void unregister_device_returns_204() {
        String token = "fcm-unreg-" + System.nanoTime();
        var req = new RegisterReq("FCM", token, "android");

        rest.exchange(
                baseUrl("/api/v1/devices/register"),
                HttpMethod.POST,
                new HttpEntity<>(req, auth(tokenA)),
                Void.class);

        ResponseEntity<Void> del =
                rest.exchange(
                        baseUrl("/api/v1/devices/" + token),
                        HttpMethod.DELETE,
                        new HttpEntity<>(auth(tokenA)),
                        Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void unauthenticated_register_returns_403() {
        var req = new RegisterReq("FCM", "token-unauth", "android");
        ResponseEntity<Void> resp =
                rest.exchange(
                        baseUrl("/api/v1/devices/register"),
                        HttpMethod.POST,
                        new HttpEntity<>(req),
                        Void.class);
        assertThat(resp.getStatusCode().value()).isIn(401, 403);
    }
}
