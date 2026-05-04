package com.hubfeatcreators.rbac;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubfeatcreators.IntegrationTestBase;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** R3 — smoke do CRUD de perfis + bypass do OWNER + perfis seed criados no signup. */
class RbacIT extends IntegrationTestBase {

    @Autowired TestRestTemplate rest;

    record SignupRequest(String assessoriaNome, String slug, String email, String senha) {}

    record TokenResponse(String accessToken, String refreshToken) {}

    record PerfilRequest(String nome, String descricao, Set<String> roles) {}

    private TokenResponse signupOwner(String slug) {
        var req = new SignupRequest("RBAC " + slug, slug, slug + "@test.com", "senha123456");
        return rest.postForEntity(baseUrl("/api/v1/auth/signup"), req, TokenResponse.class)
                .getBody();
    }

    private HttpHeaders bearer(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(token);
        return h;
    }

    @Test
    void signup_cria_3_perfis_seed_e_owner_lista_eles() {
        String slug = "rbac-seed-" + System.nanoTime();
        TokenResponse tokens = signupOwner(slug);
        assertThat(tokens).isNotNull();

        ResponseEntity<List> resp =
                rest.exchange(
                        baseUrl("/api/v1/perfis"),
                        HttpMethod.GET,
                        new HttpEntity<>(bearer(tokens.accessToken())),
                        List.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resp.getBody()).hasSize(3);
        assertThat(resp.getBody().stream().map(o -> ((Map<?, ?>) o).get("nome")))
                .containsExactlyInAnyOrder("Owner", "Assessor", "Leitor");
    }

    @Test
    void owner_consegue_criar_perfil_customizado() {
        String slug = "rbac-create-" + System.nanoTime();
        TokenResponse tokens = signupOwner(slug);

        var req = new PerfilRequest("Comercial", "Time de vendas", Set.of("BPRO", "CPRO", "EPRO"));
        ResponseEntity<Map> resp =
                rest.exchange(
                        baseUrl("/api/v1/perfis"),
                        HttpMethod.POST,
                        new HttpEntity<>(req, bearer(tokens.accessToken())),
                        Map.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody().get("nome")).isEqualTo("Comercial");
        assertThat(resp.getBody().get("isSystem")).isEqualTo(false);
    }

    @Test
    void criar_perfil_com_role_invalida_retorna_400() {
        String slug = "rbac-inv-" + System.nanoTime();
        TokenResponse tokens = signupOwner(slug);

        var req = new PerfilRequest("Bug", null, Set.of("ZZZZ"));
        ResponseEntity<Object> resp =
                rest.exchange(
                        baseUrl("/api/v1/perfis"),
                        HttpMethod.POST,
                        new HttpEntity<>(req, bearer(tokens.accessToken())),
                        Object.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void criar_perfil_com_nome_duplicado_retorna_409() {
        String slug = "rbac-dup-" + System.nanoTime();
        TokenResponse tokens = signupOwner(slug);

        var req = new PerfilRequest("Owner", "duplicado", Set.of("BPRO"));
        ResponseEntity<Object> resp =
                rest.exchange(
                        baseUrl("/api/v1/perfis"),
                        HttpMethod.POST,
                        new HttpEntity<>(req, bearer(tokens.accessToken())),
                        Object.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void deletar_perfil_sistema_retorna_400() {
        String slug = "rbac-sys-" + System.nanoTime();
        TokenResponse tokens = signupOwner(slug);

        Map<?, ?> seed =
                (Map<?, ?>)
                        rest
                                .exchange(
                                        baseUrl("/api/v1/perfis"),
                                        HttpMethod.GET,
                                        new HttpEntity<>(bearer(tokens.accessToken())),
                                        List.class)
                                .getBody()
                                .stream()
                                .filter(p -> "Leitor".equals(((Map<?, ?>) p).get("nome")))
                                .findFirst()
                                .orElseThrow();

        ResponseEntity<Object> resp =
                rest.exchange(
                        baseUrl("/api/v1/perfis/" + seed.get("id")),
                        HttpMethod.DELETE,
                        new HttpEntity<>(bearer(tokens.accessToken())),
                        Object.class);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void requisicao_sem_token_para_endpoint_protegido_retorna_401_ou_403() {
        ResponseEntity<Object> resp = rest.getForEntity(baseUrl("/api/v1/perfis"), Object.class);

        assertThat(resp.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }
}
