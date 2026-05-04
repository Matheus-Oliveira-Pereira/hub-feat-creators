package com.hubfeatcreators.multitenant;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubfeatcreators.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

class MultiTenantIsolationIT extends IntegrationTestBase {

    @Autowired TestRestTemplate rest;

    record SignupRequest(String assessoriaNome, String slug, String email, String senha) {}

    record TokenResponse(String accessToken, String refreshToken) {}

    record InfluenciadorRequest(
            String nome,
            java.util.Map<String, String> handles,
            String nicho,
            Long audienciaTotal,
            String observacoes,
            java.util.List<String> tags) {}

    record InfluenciadorResponse(java.util.UUID id, String nome) {}

    record PageResponse(java.util.List<InfluenciadorResponse> data) {}

    String tokenA;
    String tokenB;

    @BeforeEach
    void setup() {
        long ts = System.nanoTime();
        tokenA = signup("Assessoria A", "slug-a-" + ts, "a" + ts + "@test.com");
        tokenB = signup("Assessoria B", "slug-b-" + ts, "b" + ts + "@test.com");
    }

    @Test
    void assessoria_A_nao_ve_influenciadores_da_assessoria_B() {
        // Assessoria B cria influenciador
        criarInfluenciador(tokenB, "Influenciador da B");

        // Assessoria A lista — não deve ver influenciadores da B
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenA);
        ResponseEntity<PageResponse> resp =
                rest.exchange(
                        baseUrl("/api/v1/influenciadores"),
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        PageResponse.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().data()).isEmpty();
    }

    @Test
    void assessoria_A_nao_acessa_influenciador_da_B_por_id() {
        InfluenciadorResponse inf = criarInfluenciador(tokenB, "Influenciador privado da B");

        // Assessoria A tenta buscar pelo ID
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenA);
        ResponseEntity<Object> resp =
                rest.exchange(
                        baseUrl("/api/v1/influenciadores/" + inf.id()),
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        Object.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ---- helpers ----

    private String signup(String nome, String slug, String email) {
        record Req(String assessoriaNome, String slug, String email, String senha) {}
        var resp =
                rest.postForEntity(
                        baseUrl("/api/v1/auth/signup"),
                        new Req(nome, slug, email, "senha123456"),
                        TokenResponse.class);
        return resp.getBody().accessToken();
    }

    private InfluenciadorResponse criarInfluenciador(String token, String nome) {
        var req =
                new InfluenciadorRequest(
                        nome, java.util.Map.of(), null, null, null, java.util.List.of());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return rest.exchange(
                        baseUrl("/api/v1/influenciadores"),
                        HttpMethod.POST,
                        new HttpEntity<>(req, headers),
                        InfluenciadorResponse.class)
                .getBody();
    }
}
