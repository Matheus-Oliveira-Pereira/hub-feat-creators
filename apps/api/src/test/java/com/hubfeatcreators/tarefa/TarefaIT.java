package com.hubfeatcreators.tarefa;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubfeatcreators.IntegrationTestBase;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

/** i4 — IT PRD-003: CRUD + AC-2 atribuição + AC-3 feita/reabrir + AC-4/5 cross-tenant. */
class TarefaIT extends IntegrationTestBase {

    @Autowired TestRestTemplate rest;

    String token;
    String token2; // assessoria diferente para cross-tenant

    record SignupReq(String assessoriaNome, String slug, String email, String senha) {}

    record TokenResp(String accessToken, String refreshToken) {}

    record TarefaReq(
            String titulo,
            String descricao,
            String prazo,
            String prioridade,
            UUID responsavelId,
            String entidadeTipo,
            UUID entidadeId) {}

    record TarefaResp(
            UUID id,
            String titulo,
            String status,
            String prioridade,
            UUID responsavelId,
            String concluidaEm) {}

    record StatusReq(String status) {}

    record ComentarioReq(String texto) {}

    record ComentarioResp(UUID id, String texto) {}

    record PageResp(List<TarefaResp> data) {}

    @BeforeEach
    void setup() {
        long ts = System.nanoTime();
        token =
                rest.postForEntity(
                                baseUrl("/api/v1/auth/signup"),
                                new SignupReq(
                                        "Tarefa IT A",
                                        "tar-it-a-" + ts,
                                        "ta" + ts + "@test.com",
                                        "senha123456"),
                                TokenResp.class)
                        .getBody()
                        .accessToken();

        token2 =
                rest.postForEntity(
                                baseUrl("/api/v1/auth/signup"),
                                new SignupReq(
                                        "Tarefa IT B",
                                        "tar-it-b-" + ts,
                                        "tb" + ts + "@test.com",
                                        "senha123456"),
                                TokenResp.class)
                        .getBody()
                        .accessToken();
    }

    HttpHeaders h(String tok) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tok);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    TarefaResp criar(String titulo, String prazo, String tok) {
        var req = new TarefaReq(titulo, null, prazo, "MEDIA", null, null, null);
        return rest.exchange(
                        baseUrl("/api/v1/tarefas"),
                        HttpMethod.POST,
                        new HttpEntity<>(req, h(tok)),
                        TarefaResp.class)
                .getBody();
    }

    // ─── AC-1: Criar com título + prazo obrigatórios ─────────────────────────

    @Test
    void ac1_criar_tarefa_basica() {
        String prazo = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        TarefaResp t = criar("Follow-up marca X", prazo, token);
        assertThat(t.id()).isNotNull();
        assertThat(t.titulo()).isEqualTo("Follow-up marca X");
        assertThat(t.status()).isEqualTo("TODO");
        assertThat(t.prioridade()).isEqualTo("MEDIA");
    }

    @Test
    void ac1_criar_sem_titulo_retorna_400() {
        var req =
                new TarefaReq(
                        null,
                        null,
                        Instant.now().plus(1, ChronoUnit.DAYS).toString(),
                        "MEDIA",
                        null,
                        null,
                        null);
        ResponseEntity<Map> resp =
                rest.exchange(
                        baseUrl("/api/v1/tarefas"),
                        HttpMethod.POST,
                        new HttpEntity<>(req, h(token)),
                        Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(400);
    }

    // ─── AC-3: Feita registra concluidaEm; reabrir limpa ───────────────────

    @Test
    void ac3_marcar_feita_registra_concluida_em() {
        String prazo = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        TarefaResp t = criar("Tarefa para fechar", prazo, token);

        ResponseEntity<TarefaResp> resp =
                rest.exchange(
                        baseUrl("/api/v1/tarefas/" + t.id() + "/status"),
                        HttpMethod.PATCH,
                        new HttpEntity<>(new StatusReq("FEITA"), h(token)),
                        TarefaResp.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().status()).isEqualTo("FEITA");
        assertThat(resp.getBody().concluidaEm()).isNotNull();
    }

    @Test
    void ac3_reabrir_limpa_concluida_em() {
        String prazo = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        TarefaResp t = criar("Tarefa reabrir", prazo, token);

        rest.exchange(
                baseUrl("/api/v1/tarefas/" + t.id() + "/status"),
                HttpMethod.PATCH,
                new HttpEntity<>(new StatusReq("FEITA"), h(token)),
                TarefaResp.class);

        ResponseEntity<TarefaResp> resp =
                rest.exchange(
                        baseUrl("/api/v1/tarefas/" + t.id() + "/status"),
                        HttpMethod.PATCH,
                        new HttpEntity<>(new StatusReq("TODO"), h(token)),
                        TarefaResp.class);
        assertThat(resp.getBody().concluidaEm()).isNull();
        assertThat(resp.getBody().status()).isEqualTo("TODO");
    }

    // ─── AC-5/Cross-tenant: outro tenant não acessa ─────────────────────────

    @Test
    void ac5_cross_tenant_retorna_404() {
        String prazo = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        TarefaResp t = criar("Tarefa privada", prazo, token);

        ResponseEntity<Map> resp =
                rest.exchange(
                        baseUrl("/api/v1/tarefas/" + t.id()),
                        HttpMethod.GET,
                        new HttpEntity<>(h(token2)),
                        Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void ac5_cross_tenant_delete_retorna_404() {
        String prazo = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        TarefaResp t = criar("Tarefa para deletar", prazo, token);

        ResponseEntity<Map> resp =
                rest.exchange(
                        baseUrl("/api/v1/tarefas/" + t.id()),
                        HttpMethod.DELETE,
                        new HttpEntity<>(h(token2)),
                        Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(404);
    }

    // ─── AC-11: Comentários imutáveis (GET/POST; sem DELETE) ────────────────

    @Test
    void ac11_comentario_criado_e_listado() {
        String prazo = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        TarefaResp t = criar("Tarefa com comentário", prazo, token);

        ResponseEntity<ComentarioResp> c =
                rest.exchange(
                        baseUrl("/api/v1/tarefas/" + t.id() + "/comentarios"),
                        HttpMethod.POST,
                        new HttpEntity<>(
                                new ComentarioReq("Ligar para o cliente amanhã"), h(token)),
                        ComentarioResp.class);
        assertThat(c.getStatusCode().value()).isEqualTo(201);
        assertThat(c.getBody().texto()).isEqualTo("Ligar para o cliente amanhã");

        ResponseEntity<List<ComentarioResp>> lista =
                rest.exchange(
                        baseUrl("/api/v1/tarefas/" + t.id() + "/comentarios"),
                        HttpMethod.GET,
                        new HttpEntity<>(h(token)),
                        new ParameterizedTypeReference<>() {});
        assertThat(lista.getBody()).hasSize(1);
    }

    // ─── Alerta badge ────────────────────────────────────────────────────────

    @Test
    void alerta_inclui_tarefas_vencidas() {
        String passado = Instant.now().minus(1, ChronoUnit.DAYS).toString();
        criar("Tarefa atrasada", passado, token);

        ResponseEntity<Map> resp =
                rest.exchange(
                        baseUrl("/api/v1/tarefas/alerta"),
                        HttpMethod.GET,
                        new HttpEntity<>(h(token)),
                        Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(((Number) resp.getBody().get("count")).longValue()).isGreaterThan(0);
    }

    // ─── Preferências digest ─────────────────────────────────────────────────

    @Test
    void preferencias_default_digest_enabled() {
        ResponseEntity<Map> resp =
                rest.exchange(
                        baseUrl("/api/v1/tarefas/preferencias"),
                        HttpMethod.GET,
                        new HttpEntity<>(h(token)),
                        Map.class);
        assertThat(resp.getStatusCode().value()).isEqualTo(200);
        assertThat(resp.getBody().get("digestDiarioEnabled")).isEqualTo(true);
    }

    @Test
    void preferencias_optout_digest() {
        rest.exchange(
                baseUrl("/api/v1/tarefas/preferencias"),
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("digestDiarioEnabled", false), h(token)),
                Map.class);

        ResponseEntity<Map> resp =
                rest.exchange(
                        baseUrl("/api/v1/tarefas/preferencias"),
                        HttpMethod.GET,
                        new HttpEntity<>(h(token)),
                        Map.class);
        assertThat(resp.getBody().get("digestDiarioEnabled")).isEqualTo(false);
    }
}
