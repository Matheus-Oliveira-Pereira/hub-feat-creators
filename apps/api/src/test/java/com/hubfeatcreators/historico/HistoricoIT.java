package com.hubfeatcreators.historico;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubfeatcreators.IntegrationTestBase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

class HistoricoIT extends IntegrationTestBase {

    @Autowired TestRestTemplate rest;

    String tokenA;
    String tokenB;
    String prospeccaoIdA;

    record SignupReq(String assessoriaNome, String slug, String email, String senha) {}

    record TokenResp(String accessToken) {}

    record MarcaReq(String nome, String baseLegal) {}

    record MarcaResp(String id) {}

    record InflReq(String nome, Map<String, String> handles, String baseLegal) {}

    record InflResp(String id) {}

    record ProspReq(String marcaId, String titulo) {}

    record ProspResp(String id) {}

    record Pagination(String cursor, boolean hasMore, int limit) {}

    record PageResp<T>(List<T> data, Pagination pagination) {}

    record EventoItem(String id, String tipo, String ts) {}

    @BeforeEach
    void setup() {
        long ts = System.nanoTime();
        tokenA = signup("hist-a-" + ts, "hist.a" + ts + "@test.com");
        tokenB = signup("hist-b-" + ts, "hist.b" + ts + "@test.com");

        // Create a marca and prospeccao for tenant A
        var marcaResp =
                rest.exchange(
                                baseUrl("/api/v1/marcas"),
                                HttpMethod.POST,
                                new HttpEntity<>(
                                        new MarcaReq("MarcaHist", "EXECUCAO_CONTRATO"), h(tokenA)),
                                MarcaResp.class)
                        .getBody();
        assertThat(marcaResp).isNotNull();

        var prospResp =
                rest.exchange(
                                baseUrl("/api/v1/prospeccoes"),
                                HttpMethod.POST,
                                new HttpEntity<>(
                                        Map.of("marcaId", marcaResp.id(), "titulo", "Hist Prosp"),
                                        h(tokenA)),
                                ProspResp.class)
                        .getBody();
        assertThat(prospResp).isNotNull();
        prospeccaoIdA = prospResp.id();
    }

    String signup(String slug, String email) {
        var resp =
                rest.postForEntity(
                        baseUrl("/api/v1/auth/signup"),
                        new SignupReq("Assessoria " + slug, slug, email, "senha123456"),
                        TokenResp.class);
        assertThat(resp.getBody()).isNotNull();
        return resp.getBody().accessToken();
    }

    HttpHeaders h(String token) {
        HttpHeaders hh = new HttpHeaders();
        hh.setBearerAuth(token);
        hh.setContentType(MediaType.APPLICATION_JSON);
        return hh;
    }

    @Test
    void criar_prospeccao_gera_evento_no_historico() {
        var resp =
                rest.exchange(
                        baseUrl(
                                "/api/v1/historico?entidade_tipo=PROSPECCAO&entidade_id="
                                        + prospeccaoIdA),
                        HttpMethod.GET,
                        new HttpEntity<>(h(tokenA)),
                        new ParameterizedTypeReference<PageResp<EventoItem>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().data()).isNotEmpty();

        boolean hasCriada =
                resp.getBody().data().stream().anyMatch(e -> "PROSPECCAO_CRIADA".equals(e.tipo()));
        assertThat(hasCriada).isTrue();
    }

    @Test
    void filtro_tipo_exclui_outros_eventos() {
        var resp =
                rest.exchange(
                        baseUrl(
                                "/api/v1/historico?entidade_tipo=PROSPECCAO&entidade_id="
                                        + prospeccaoIdA
                                        + "&tipos=TAREFA_CRIADA"),
                        HttpMethod.GET,
                        new HttpEntity<>(h(tokenA)),
                        new ParameterizedTypeReference<PageResp<EventoItem>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        // No TAREFA_CRIADA events for this prospeccao
        assertThat(resp.getBody().data()).isEmpty();
    }

    @Test
    void tenant_b_nao_ve_historico_do_tenant_a() {
        var resp =
                rest.exchange(
                        baseUrl(
                                "/api/v1/historico?entidade_tipo=PROSPECCAO&entidade_id="
                                        + prospeccaoIdA),
                        HttpMethod.GET,
                        new HttpEntity<>(h(tokenB)),
                        new ParameterizedTypeReference<PageResp<EventoItem>>() {});

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        // Tenant B is owner of their own assessoria but events belong to tenant A's assessoriaId
        // → list returns empty (RBAC filter by assessoriaId)
        assertThat(resp.getBody().data()).isEmpty();
    }
}
