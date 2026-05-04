package com.hubfeatcreators.influenciador;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubfeatcreators.IntegrationTestBase;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

class InfluenciadorIT extends IntegrationTestBase {

    @Autowired TestRestTemplate rest;

    String token;

    record SignupReq(String assessoriaNome, String slug, String email, String senha) {}

    record TokenResp(String accessToken, String refreshToken) {}

    record InfReq(
            String nome,
            Map<String, String> handles,
            String nicho,
            Long audienciaTotal,
            String observacoes,
            List<String> tags,
            String baseLegal) {}

    record InfResp(UUID id, String nome, String nicho, List<String> tags) {}

    record PageResp(List<InfResp> data) {}

    @BeforeEach
    void setup() {
        long ts = System.nanoTime();
        token =
                rest.postForEntity(
                                baseUrl("/api/v1/auth/signup"),
                                new SignupReq(
                                        "Agency IT",
                                        "agency-it-" + ts,
                                        "it" + ts + "@test.com",
                                        "senha123456"),
                                TokenResp.class)
                        .getBody()
                        .accessToken();
    }

    @Test
    void crud_completo_influenciador() {
        // CREATE
        var req =
                new InfReq(
                        "João Creator",
                        Map.of("instagram", "@joao"),
                        "fitness",
                        50000L,
                        "obs",
                        List.of("top", "fitness"),
                        "LEGITIMO_INTERESSE");
        HttpHeaders h = headers();
        InfResp created =
                rest.exchange(
                                baseUrl("/api/v1/influenciadores"),
                                HttpMethod.POST,
                                new HttpEntity<>(req, h),
                                InfResp.class)
                        .getBody();

        assertThat(created).isNotNull();
        assertThat(created.nome()).isEqualTo("João Creator");
        assertThat(created.nicho()).isEqualTo("fitness");

        // GET by id
        InfResp fetched =
                rest.exchange(
                                baseUrl("/api/v1/influenciadores/" + created.id()),
                                HttpMethod.GET,
                                new HttpEntity<>(h),
                                InfResp.class)
                        .getBody();
        assertThat(fetched.nome()).isEqualTo("João Creator");

        // UPDATE
        var upd =
                new InfReq(
                        "João Creator Updated",
                        Map.of("instagram", "@joaonew"),
                        "moda",
                        60000L,
                        "obs2",
                        List.of("moda"),
                        "LEGITIMO_INTERESSE");
        InfResp updated =
                rest.exchange(
                                baseUrl("/api/v1/influenciadores/" + created.id()),
                                HttpMethod.PUT,
                                new HttpEntity<>(upd, h),
                                InfResp.class)
                        .getBody();
        assertThat(updated.nome()).isEqualTo("João Creator Updated");

        // DELETE (soft)
        ResponseEntity<Void> del =
                rest.exchange(
                        baseUrl("/api/v1/influenciadores/" + created.id()),
                        HttpMethod.DELETE,
                        new HttpEntity<>(h),
                        Void.class);
        assertThat(del.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Não aparece na listagem
        PageResp page =
                rest.exchange(
                                baseUrl("/api/v1/influenciadores"),
                                HttpMethod.GET,
                                new HttpEntity<>(h),
                                PageResp.class)
                        .getBody();
        assertThat(page.data()).noneMatch(i -> i.id().equals(created.id()));
    }

    @Test
    void busca_por_nome_retorna_resultados_corretos() {
        HttpHeaders h = headers();
        rest.exchange(
                baseUrl("/api/v1/influenciadores"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new InfReq("Maria Fitness", Map.of(), "fitness", null, null, List.of(), "LEGITIMO_INTERESSE"), h),
                InfResp.class);
        rest.exchange(
                baseUrl("/api/v1/influenciadores"),
                HttpMethod.POST,
                new HttpEntity<>(
                        new InfReq("Pedro Gamer", Map.of(), "games", null, null, List.of(), "LEGITIMO_INTERESSE"), h),
                InfResp.class);

        PageResp page =
                rest.exchange(
                                baseUrl("/api/v1/influenciadores?nome=Maria"),
                                HttpMethod.GET,
                                new HttpEntity<>(h),
                                PageResp.class)
                        .getBody();

        assertThat(page.data()).anyMatch(i -> i.nome().contains("Maria"));
        assertThat(page.data()).noneMatch(i -> i.nome().contains("Pedro"));
    }

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}
