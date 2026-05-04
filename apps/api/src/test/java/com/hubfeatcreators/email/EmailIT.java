package com.hubfeatcreators.email;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubfeatcreators.IntegrationTestBase;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

/**
 * IT PRD-004 email outbound: AC-1 criar conta + AC-2 testar conexão + AC-3 template CRUD + AC-4
 * enviar AC-5 opt-out bloqueia envio + AC-6 quota bloqueia envio + AC-7 cross-tenant 404
 */
class EmailIT extends IntegrationTestBase {

    @RegisterExtension
    static GreenMailExtension greenMail =
            new GreenMailExtension(ServerSetupTest.SMTP)
                    .withConfiguration(
                            com.icegreen.greenmail.configuration.GreenMailConfiguration.aConfig()
                                    .withUser("relayuser", "relaypass"))
                    .withPerMethodLifecycle(false);

    @Autowired TestRestTemplate rest;

    String token;
    String token2;

    record SignupReq(String assessoriaNome, String slug, String email, String senha) {}

    record TokenResp(String accessToken, String refreshToken) {}

    record AccountReq(
            String nome,
            String host,
            Integer port,
            String username,
            String password,
            String fromAddress,
            String fromName,
            String tlsMode,
            Integer dailyQuota) {}

    record AccountResp(String id, String nome, String status, Integer falhasAuthCount) {}

    record TemplateReq(
            String nome, String assunto, String corpoHtml, String corpoTexto, String[] variaveis) {}

    record TemplateResp(String id, String nome) {}

    record EnvioReq(
            String accountId,
            String templateId,
            String destinatarioEmail,
            String destinatarioNome,
            Map<String, Object> vars,
            Map<String, Object> contexto,
            String idempotencyKey,
            boolean trackingEnabled) {}

    record EnvioResp(String id, String status) {}

    @BeforeEach
    void setup() {
        long ts = System.nanoTime();
        token =
                rest.postForEntity(
                                baseUrl("/api/v1/auth/signup"),
                                new SignupReq(
                                        "Email IT A",
                                        "email-it-a-" + ts,
                                        "ea" + ts + "@test.com",
                                        "senha123456"),
                                TokenResp.class)
                        .getBody()
                        .accessToken();

        token2 =
                rest.postForEntity(
                                baseUrl("/api/v1/auth/signup"),
                                new SignupReq(
                                        "Email IT B",
                                        "email-it-b-" + ts,
                                        "eb" + ts + "@test.com",
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

    @Test
    void ac1_criar_conta_retorna_201() {
        var req =
                new AccountReq(
                        "Gmail A1",
                        "127.0.0.1",
                        ServerSetupTest.SMTP.getPort(),
                        "relayuser",
                        "relaypass",
                        "from@test.com",
                        "Test Sender",
                        "STARTTLS",
                        500);

        ResponseEntity<AccountResp> res =
                rest.exchange(
                        baseUrl("/api/v1/email/accounts"),
                        HttpMethod.POST,
                        new HttpEntity<>(req, h(token)),
                        AccountResp.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody().nome()).isEqualTo("Gmail A1");
        assertThat(res.getBody().status()).isEqualTo("ATIVA");
    }

    @Test
    void ac2_testar_conexao_greenmail_ok() {
        // Create account pointing to GreenMail
        var accountReq =
                new AccountReq(
                        "GreenMail",
                        "127.0.0.1",
                        ServerSetupTest.SMTP.getPort(),
                        "relayuser",
                        "relaypass",
                        "from@test.com",
                        "Test",
                        "STARTTLS",
                        500);
        var account =
                rest.exchange(
                                baseUrl("/api/v1/email/accounts"),
                                HttpMethod.POST,
                                new HttpEntity<>(accountReq, h(token)),
                                AccountResp.class)
                        .getBody();

        ResponseEntity<Void> res =
                rest.exchange(
                        baseUrl("/api/v1/email/accounts/" + account.id() + "/test"),
                        HttpMethod.POST,
                        new HttpEntity<>(null, h(token)),
                        Void.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void ac3_template_crud_e_preview() {
        var req =
                new TemplateReq(
                        "Boas-vindas",
                        "Bem-vindo!",
                        "<h1>Olá {{nome}}</h1>",
                        "Olá {{nome}}",
                        new String[] {"nome"});

        var template =
                rest.exchange(
                                baseUrl("/api/v1/email/templates"),
                                HttpMethod.POST,
                                new HttpEntity<>(req, h(token)),
                                TemplateResp.class)
                        .getBody();
        assertThat(template.nome()).isEqualTo("Boas-vindas");

        var preview =
                rest.exchange(
                                baseUrl("/api/v1/email/templates/" + template.id() + "/preview"),
                                HttpMethod.POST,
                                new HttpEntity<>(
                                        Map.of("vars", Map.of("nome", "Matheus")), h(token)),
                                Map.class)
                        .getBody();
        assertThat(preview.get("html").toString()).contains("Olá Matheus");
    }

    @Test
    void ac4_envio_enfileirado_com_idempotencia() {
        var account = criarConta(token);
        var template = criarTemplate(token);
        UUID idemKey = UUID.randomUUID();

        var req =
                new EnvioReq(
                        account.id(),
                        template.id(),
                        "dest@test.com",
                        "Destinatário",
                        Map.of("nome", "X"),
                        Map.of(),
                        idemKey.toString(),
                        false);

        var r1 =
                rest.exchange(
                                baseUrl("/api/v1/email/envios"),
                                HttpMethod.POST,
                                new HttpEntity<>(req, h(token)),
                                EnvioResp.class)
                        .getBody();
        assertThat(r1.status()).isEqualTo("ENFILEIRADO");

        // Same idempotency key → same envio ID
        var r2 =
                rest.exchange(
                                baseUrl("/api/v1/email/envios"),
                                HttpMethod.POST,
                                new HttpEntity<>(req, h(token)),
                                EnvioResp.class)
                        .getBody();
        assertThat(r2.id()).isEqualTo(r1.id());
    }

    @Test
    void ac5_optout_bloqueia_envio() {
        var account = criarConta(token);
        var template = criarTemplate(token);
        String email = "optout" + System.nanoTime() + "@test.com";

        // Simulate unsubscribe via tracking endpoint
        rest.getForEntity(
                baseUrl(
                        "/api/v1/email/unsubscribe?token="
                                + java.util.Base64.getUrlEncoder()
                                        .withoutPadding()
                                        .encodeToString(
                                                (extractAssessoriaId(token) + ":" + email)
                                                        .getBytes(
                                                                java.nio.charset.StandardCharsets
                                                                        .UTF_8))),
                String.class);

        var req =
                new EnvioReq(
                        account.id(),
                        template.id(),
                        email,
                        "Opt-Out",
                        Map.of(),
                        Map.of(),
                        UUID.randomUUID().toString(),
                        false);
        ResponseEntity<String> res =
                rest.exchange(
                        baseUrl("/api/v1/email/envios"),
                        HttpMethod.POST,
                        new HttpEntity<>(req, h(token)),
                        String.class);

        assertThat(res.getStatusCode().value()).isEqualTo(422);
    }

    @Test
    void ac6_cross_tenant_404() {
        var account = criarConta(token);
        // token2 (different assessoria) cannot see token's account
        ResponseEntity<String> res =
                rest.exchange(
                        baseUrl("/api/v1/email/accounts/" + account.id()),
                        HttpMethod.GET,
                        new HttpEntity<>(h(token2)),
                        String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    AccountResp criarConta(String tok) {
        var req =
                new AccountReq(
                        "Test Account",
                        "127.0.0.1",
                        ServerSetupTest.SMTP.getPort(),
                        "relayuser",
                        "relaypass",
                        "from@test.com",
                        "Sender",
                        "STARTTLS",
                        500);
        return rest.exchange(
                        baseUrl("/api/v1/email/accounts"),
                        HttpMethod.POST,
                        new HttpEntity<>(req, h(tok)),
                        AccountResp.class)
                .getBody();
    }

    TemplateResp criarTemplate(String tok) {
        var req =
                new TemplateReq(
                        "T" + System.nanoTime(),
                        "Assunto",
                        "<p>Olá {{nome}}</p>",
                        null,
                        new String[] {"nome"});
        return rest.exchange(
                        baseUrl("/api/v1/email/templates"),
                        HttpMethod.POST,
                        new HttpEntity<>(req, h(tok)),
                        TemplateResp.class)
                .getBody();
    }

    String extractAssessoriaId(String tok) {
        // JWT payload is base64 — extract assessoriaId claim
        String[] parts = tok.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        // Simple substring extraction: "assessoriaId":"<uuid>"
        int start = payload.indexOf("\"assessoriaId\":\"") + 16;
        return payload.substring(start, start + 36);
    }
}
