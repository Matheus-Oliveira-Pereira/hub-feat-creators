package com.hubfeatcreators.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubfeatcreators.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthIT extends IntegrationTestBase {

  @Autowired TestRestTemplate rest;

  record SignupRequest(String assessoriaNome, String slug, String email, String senha) {}
  record LoginRequest(String email, String senha) {}
  record TokenResponse(String accessToken, String refreshToken) {}
  record RefreshRequest(String refreshToken) {}
  record LogoutRequest(String refreshToken) {}

  @Test
  void signup_cria_assessoria_e_retorna_tokens() {
    var req = new SignupRequest("Test Agency", "test-agency-" + System.nanoTime(),
        "owner@test.com", "senha123456");
    ResponseEntity<TokenResponse> resp = rest.postForEntity(
        baseUrl("/api/v1/auth/signup"), req, TokenResponse.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(resp.getBody()).isNotNull();
    assertThat(resp.getBody().accessToken()).isNotBlank();
    assertThat(resp.getBody().refreshToken()).isNotBlank();
  }

  @Test
  void signup_slug_duplicado_retorna_409() {
    String slug = "slug-dup-" + System.nanoTime();
    var req = new SignupRequest("Agency A", slug, "a@test.com", "senha123456");
    rest.postForEntity(baseUrl("/api/v1/auth/signup"), req, Object.class);

    var req2 = new SignupRequest("Agency B", slug, "b@test.com", "senha123456");
    ResponseEntity<Object> resp = rest.postForEntity(
        baseUrl("/api/v1/auth/signup"), req2, Object.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void login_credenciais_invalidas_retorna_401() {
    var req = new LoginRequest("naoexiste@test.com", "wrongpassword");
    ResponseEntity<Object> resp = rest.postForEntity(
        baseUrl("/api/v1/auth/login"), req, Object.class);

    assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }

  @Test
  void refresh_rotation_funciona() {
    String slug = "refresh-" + System.nanoTime();
    var signup = new SignupRequest("Agency R", slug, "r@test.com", "senha123456");
    TokenResponse tokens = rest.postForEntity(
        baseUrl("/api/v1/auth/signup"), signup, TokenResponse.class).getBody();

    assertThat(tokens).isNotNull();
    ResponseEntity<TokenResponse> refreshResp = rest.postForEntity(
        baseUrl("/api/v1/auth/refresh"),
        new RefreshRequest(tokens.refreshToken()),
        TokenResponse.class);

    assertThat(refreshResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(refreshResp.getBody()).isNotNull();
    assertThat(refreshResp.getBody().refreshToken()).isNotEqualTo(tokens.refreshToken());
  }

  @Test
  void refresh_token_reusado_retorna_401_e_revoga_familia() {
    String slug = "reuse-" + System.nanoTime();
    var signup = new SignupRequest("Agency X", slug, "x@test.com", "senha123456");
    TokenResponse tokens = rest.postForEntity(
        baseUrl("/api/v1/auth/signup"), signup, TokenResponse.class).getBody();

    // Primeiro refresh válido
    rest.postForEntity(baseUrl("/api/v1/auth/refresh"),
        new RefreshRequest(tokens.refreshToken()), TokenResponse.class);

    // Reusar o refresh original (revogado) → deve retornar 401
    ResponseEntity<Object> reuseResp = rest.postForEntity(
        baseUrl("/api/v1/auth/refresh"),
        new RefreshRequest(tokens.refreshToken()),
        Object.class);

    assertThat(reuseResp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
  }
}
