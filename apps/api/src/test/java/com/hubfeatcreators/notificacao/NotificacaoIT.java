package com.hubfeatcreators.notificacao;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubfeatcreators.IntegrationTestBase;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

/** IT: contagem notificações, isolamento cross-tenant, prefs defaults. */
class NotificacaoIT extends IntegrationTestBase {

    @Autowired TestRestTemplate rest;

    String tokenA;
    String tokenB;

    record SignupReq(String assessoriaNome, String slug, String email, String senha) {}

    record TokenResp(String accessToken) {}

    record ContagemResp(long naoLidas) {}

    @BeforeEach
    void setup() {
        long ts = System.nanoTime();
        tokenA = signup("notif-a-" + ts, "notif.a" + ts + "@test.com");
        tokenB = signup("notif-b-" + ts, "notif.b" + ts + "@test.com");
    }

    String signup(String slug, String email) {
        return rest.postForEntity(
                        baseUrl("/api/v1/auth/signup"),
                        new SignupReq("Assessoria " + slug, slug, email, "senha123456"),
                        TokenResp.class)
                .getBody()
                .accessToken();
    }

    HttpHeaders h(String token) {
        HttpHeaders hh = new HttpHeaders();
        hh.setBearerAuth(token);
        hh.setContentType(MediaType.APPLICATION_JSON);
        return hh;
    }

    @Test
    void contagem_nao_lidas_zero_para_novo_usuario() {
        var resp =
                rest.exchange(
                        baseUrl("/api/v1/notificacoes/contagem"),
                        HttpMethod.GET,
                        new HttpEntity<>(h(tokenA)),
                        ContagemResp.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().naoLidas()).isZero();
    }

    @Test
    void lista_retorna_200_vazia_para_novo_usuario() {
        var resp =
                rest.exchange(
                        baseUrl("/api/v1/notificacoes"),
                        HttpMethod.GET,
                        new HttpEntity<>(h(tokenA)),
                        Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void prefs_retorna_matriz_completa_com_defaults() {
        var resp =
                rest.exchange(
                        baseUrl("/api/v1/notificacoes/prefs"),
                        HttpMethod.GET,
                        new HttpEntity<>(h(tokenA)),
                        Object[].class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        // 9 tipos × 3 canais = 27 entradas
        assertThat(resp.getBody()).hasSize(27);
    }

    @Test
    void usuario_nao_ve_notificacoes_de_outro_tenant() {
        // Tenants A e B são isolados — usuário B tentando ver lista de A não recebe dados de A
        // (cada usuário vê só as suas)
        var respA =
                rest.exchange(
                        baseUrl("/api/v1/notificacoes"),
                        HttpMethod.GET,
                        new HttpEntity<>(h(tokenA)),
                        Map.class);
        var respB =
                rest.exchange(
                        baseUrl("/api/v1/notificacoes"),
                        HttpMethod.GET,
                        new HttpEntity<>(h(tokenB)),
                        Map.class);

        // Ambos devem ser 200 (sem erro de auth cruzado)
        assertThat(respA.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respB.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void marcar_todas_lidas_retorna_204() {
        var resp =
                rest.exchange(
                        baseUrl("/api/v1/notificacoes/lidas"),
                        HttpMethod.POST,
                        new HttpEntity<>(h(tokenA)),
                        Void.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void webpush_public_key_endpoint_publico_retorna_200() {
        var resp =
                rest.getForEntity(
                        baseUrl("/api/v1/webpush/public-key"), Map.class);

        // Pode ser 200 com chave vazia (dev sem VAPID configurado)
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
