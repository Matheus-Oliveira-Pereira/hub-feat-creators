package com.hubfeatcreators.compliance;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubfeatcreators.IntegrationTestBase;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

class ComplianceIT extends IntegrationTestBase {

    @Autowired TestRestTemplate rest;

    String token;

    record SignupReq(String assessoriaNome, String slug, String email, String senha) {}
    record TokenResp(String accessToken) {}

    @BeforeEach
    void setup() {
        long ts = System.nanoTime();
        token = rest.postForEntity(
                baseUrl("/api/v1/auth/signup"),
                new SignupReq("Compliance Test", "compliance-" + ts, "comp" + ts + "@test.com", "senha123456"),
                TokenResp.class)
                .getBody().accessToken();
    }

    @Test
    void criar_influenciador_sem_base_legal_retorna_422() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // No baseLegal field → should fail validation
        String body = """
                {"nome": "Sem Base Legal", "handles": {}, "nicho": "fitness", "tags": []}
                """;

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl("/api/v1/influenciadores"),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(resp.getStatusCode().value()).isIn(400, 422);
    }

    @Test
    void criar_influenciador_com_base_legal_valida_retorna_201() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"nome": "Com Base Legal", "handles": {}, "nicho": "moda",
                 "tags": [], "baseLegal": "LEGITIMO_INTERESSE"}
                """;

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl("/api/v1/influenciadores"),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void criar_marca_sem_base_legal_retorna_erro() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = """
                {"nome": "Marca sem base", "segmento": "tech", "tags": []}
                """;

        ResponseEntity<Map> resp = rest.exchange(
                baseUrl("/api/v1/marcas"),
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class);

        assertThat(resp.getStatusCode().value()).isIn(400, 422);
    }

    @Test
    void dsr_token_invalido_retorna_404() {
        ResponseEntity<Map> resp = rest.postForEntity(
                baseUrl("/api/v1/dsr/execute/token-invalido-nao-existe"),
                null,
                Map.class);

        assertThat(resp.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void ropa_endpoint_requer_autenticacao() {
        ResponseEntity<Map> resp = rest.getForEntity(
                baseUrl("/api/v1/admin/compliance/ropa"), Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(403);
    }
}
