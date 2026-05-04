package com.hubfeatcreators.prospeccao;

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

/** I5 — IT smoke do CRUD + state machine + visibility de prospecção. */
class ProspeccaoIT extends IntegrationTestBase {

    @Autowired TestRestTemplate rest;

    String token;

    record SignupReq(String assessoriaNome, String slug, String email, String senha) {}

    record TokenResp(String accessToken, String refreshToken) {}

    record MarcaReq(
            String nome, String segmento, String site, String observacoes, List<String> tags) {}

    record MarcaResp(UUID id, String nome) {}

    record ProspReq(
            UUID marcaId,
            UUID influenciadorId,
            UUID assessorResponsavelId,
            String titulo,
            Long valorEstimadoCentavos,
            String proximaAcao,
            String proximaAcaoEm,
            String observacoes,
            String[] tags) {}

    record ProspResp(UUID id, UUID marcaId, String titulo, String status) {}

    record StatusReq(String status, String motivoPerda, String motivoPerdaDetalhe) {}

    record PageResp(List<ProspResp> data) {}

    @BeforeEach
    void setup() {
        long ts = System.nanoTime();
        token =
                rest.postForEntity(
                                baseUrl("/api/v1/auth/signup"),
                                new SignupReq(
                                        "Prospec IT",
                                        "prospec-it-" + ts,
                                        "p" + ts + "@test.com",
                                        "senha123456"),
                                TokenResp.class)
                        .getBody()
                        .accessToken();
    }

    HttpHeaders h() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    UUID criarMarca() {
        var req = new MarcaReq("Marca " + UUID.randomUUID(), null, null, null, List.of());
        return rest.exchange(
                        baseUrl("/api/v1/marcas"),
                        HttpMethod.POST,
                        new HttpEntity<>(req, h()),
                        MarcaResp.class)
                .getBody()
                .id();
    }

    ProspResp criarProsp(UUID marcaId, String titulo) {
        var req =
                new ProspReq(
                        marcaId, null, null, titulo, 100_000L, null, null, null, new String[0]);
        return rest.exchange(
                        baseUrl("/api/v1/prospeccoes"),
                        HttpMethod.POST,
                        new HttpEntity<>(req, h()),
                        ProspResp.class)
                .getBody();
    }

    @Test
    void crud_basico_funciona() {
        UUID marcaId = criarMarca();
        ProspResp p = criarProsp(marcaId, "Campanha A");
        assertThat(p.id()).isNotNull();
        assertThat(p.status()).isEqualTo("NOVA");

        // list
        PageResp page =
                rest.exchange(
                                baseUrl("/api/v1/prospeccoes"),
                                HttpMethod.GET,
                                new HttpEntity<>(h()),
                                PageResp.class)
                        .getBody();
        assertThat(page.data()).hasSize(1);

        // get
        ProspResp got =
                rest.exchange(
                                baseUrl("/api/v1/prospeccoes/" + p.id()),
                                HttpMethod.GET,
                                new HttpEntity<>(h()),
                                ProspResp.class)
                        .getBody();
        assertThat(got.titulo()).isEqualTo("Campanha A");

        // delete (soft)
        rest.exchange(
                baseUrl("/api/v1/prospeccoes/" + p.id()),
                HttpMethod.DELETE,
                new HttpEntity<>(h()),
                Void.class);

        // listing não retorna mais
        PageResp afterDelete =
                rest.exchange(
                                baseUrl("/api/v1/prospeccoes"),
                                HttpMethod.GET,
                                new HttpEntity<>(h()),
                                PageResp.class)
                        .getBody();
        assertThat(afterDelete.data()).isEmpty();
    }

    @Test
    void mudanca_status_valida_segue_state_machine() {
        UUID marcaId = criarMarca();
        ProspResp p = criarProsp(marcaId, "Campanha SM");

        // NOVA → CONTATADA
        var resp1 =
                rest.exchange(
                        baseUrl("/api/v1/prospeccoes/" + p.id() + "/status"),
                        HttpMethod.PATCH,
                        new HttpEntity<>(new StatusReq("CONTATADA", null, null), h()),
                        ProspResp.class);
        assertThat(resp1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp1.getBody().status()).isEqualTo("CONTATADA");

        // CONTATADA → NEGOCIANDO
        rest.exchange(
                baseUrl("/api/v1/prospeccoes/" + p.id() + "/status"),
                HttpMethod.PATCH,
                new HttpEntity<>(new StatusReq("NEGOCIANDO", null, null), h()),
                ProspResp.class);

        // NEGOCIANDO → FECHADA_GANHA
        var resp3 =
                rest.exchange(
                        baseUrl("/api/v1/prospeccoes/" + p.id() + "/status"),
                        HttpMethod.PATCH,
                        new HttpEntity<>(new StatusReq("FECHADA_GANHA", null, null), h()),
                        ProspResp.class);
        assertThat(resp3.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp3.getBody().status()).isEqualTo("FECHADA_GANHA");
    }

    @Test
    void transicao_invalida_retorna_422() {
        UUID marcaId = criarMarca();
        ProspResp p = criarProsp(marcaId, "Campanha 422");
        // NOVA → FECHADA_GANHA (inválida — pula CONTATADA + NEGOCIANDO)
        ResponseEntity<Object> resp =
                rest.exchange(
                        baseUrl("/api/v1/prospeccoes/" + p.id() + "/status"),
                        HttpMethod.PATCH,
                        new HttpEntity<>(new StatusReq("FECHADA_GANHA", null, null), h()),
                        Object.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void fechada_perdida_sem_motivo_retorna_422() {
        UUID marcaId = criarMarca();
        ProspResp p = criarProsp(marcaId, "Campanha SP");
        rest.exchange(
                baseUrl("/api/v1/prospeccoes/" + p.id() + "/status"),
                HttpMethod.PATCH,
                new HttpEntity<>(new StatusReq("CONTATADA", null, null), h()),
                ProspResp.class);

        ResponseEntity<Object> resp =
                rest.exchange(
                        baseUrl("/api/v1/prospeccoes/" + p.id() + "/status"),
                        HttpMethod.PATCH,
                        new HttpEntity<>(new StatusReq("FECHADA_PERDIDA", null, null), h()),
                        Object.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void comentario_imutavel_aparece_em_eventos() {
        UUID marcaId = criarMarca();
        ProspResp p = criarProsp(marcaId, "Campanha CMT");

        rest.exchange(
                baseUrl("/api/v1/prospeccoes/" + p.id() + "/comentarios"),
                HttpMethod.POST,
                new HttpEntity<>(Map.of("texto", "primeiro comentário"), h()),
                Object.class);

        Object[] eventos =
                rest.exchange(
                                baseUrl("/api/v1/prospeccoes/" + p.id() + "/eventos"),
                                HttpMethod.GET,
                                new HttpEntity<>(h()),
                                Object[].class)
                        .getBody();

        // pelo menos: STATUS_CHANGE inicial + COMMENT
        assertThat(eventos.length).isGreaterThanOrEqualTo(2);
    }

    @Test
    void cross_tenant_retorna_404() {
        UUID marcaId = criarMarca();
        ProspResp p = criarProsp(marcaId, "Campanha X");

        // outro tenant
        long ts = System.nanoTime();
        String otherToken =
                rest.postForEntity(
                                baseUrl("/api/v1/auth/signup"),
                                new SignupReq(
                                        "Other",
                                        "other-" + ts,
                                        "o" + ts + "@test.com",
                                        "senha123456"),
                                TokenResp.class)
                        .getBody()
                        .accessToken();

        HttpHeaders otherH = new HttpHeaders();
        otherH.setBearerAuth(otherToken);

        ResponseEntity<Object> resp =
                rest.exchange(
                        baseUrl("/api/v1/prospeccoes/" + p.id()),
                        HttpMethod.GET,
                        new HttpEntity<>(otherH),
                        Object.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
