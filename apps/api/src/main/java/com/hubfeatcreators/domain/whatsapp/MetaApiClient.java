package com.hubfeatcreators.domain.whatsapp;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class MetaApiClient {

    private static final Logger log = LoggerFactory.getLogger(MetaApiClient.class);
    private static final String BASE = "https://graph.facebook.com/v20.0";

    private final RestClient rest;

    public MetaApiClient(RestClient.Builder builder) {
        this.rest = builder.baseUrl(BASE).build();
    }

    /** Validates token and returns WABA display name. Throws on 401/403. */
    public String validateToken(String accessToken) {
        try {
            JsonNode resp = rest.get()
                    .uri("/me?fields=name&access_token={token}", accessToken)
                    .retrieve()
                    .body(JsonNode.class);
            return resp != null && resp.has("name") ? resp.get("name").asText() : "unknown";
        } catch (HttpClientErrorException e) {
            throw new MetaApiException("Token inválido ou expirado: " + e.getStatusCode(), e);
        }
    }

    /** Submits a template to Meta for approval. Returns meta_template_id. */
    public String submitTemplate(String wabaId, String accessToken,
            String nome, String idioma, String categoria, String corpo) {
        Map<String, Object> body = Map.of(
                "name", nome,
                "language", idioma,
                "category", categoria,
                "components", List.of(Map.of("type", "BODY", "text", corpo))
        );
        try {
            JsonNode resp = rest.post()
                    .uri("/{wabaId}/message_templates?access_token={token}", wabaId, accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (resp == null || !resp.has("id")) throw new MetaApiException("Meta não retornou id do template");
            return resp.get("id").asText();
        } catch (HttpClientErrorException e) {
            throw new MetaApiException("Erro ao submeter template: " + e.getResponseBodyAsString(), e);
        }
    }

    /** Polls template status from Meta. Returns "APPROVED", "REJECTED", "PENDING", etc. */
    public String getTemplateStatus(String wabaId, String accessToken, String metaTemplateId) {
        try {
            JsonNode resp = rest.get()
                    .uri("/{wabaId}/message_templates?name={id}&access_token={token}",
                            wabaId, metaTemplateId, accessToken)
                    .retrieve()
                    .body(JsonNode.class);
            if (resp != null && resp.has("data") && resp.get("data").isArray()
                    && !resp.get("data").isEmpty()) {
                JsonNode first = resp.get("data").get(0);
                return first.path("status").asText("PENDING");
            }
            return "PENDING";
        } catch (HttpClientErrorException e) {
            log.warn("Meta template poll failed: {}", e.getMessage());
            return "PENDING";
        }
    }

    /** Sends a template message. Returns wamid. */
    public String sendTemplate(String phoneNumberId, String accessToken,
            String toE164, String templateNome, String idioma,
            List<Map<String, Object>> components) {
        Map<String, Object> template = Map.of(
                "name", templateNome,
                "language", Map.of("code", idioma),
                "components", components
        );
        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", toE164,
                "type", "template",
                "template", template
        );
        return doSend(phoneNumberId, accessToken, body);
    }

    /** Sends a freeform text message. Returns wamid. */
    public String sendText(String phoneNumberId, String accessToken, String toE164, String text) {
        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", toE164,
                "type", "text",
                "text", Map.of("body", text)
        );
        return doSend(phoneNumberId, accessToken, body);
    }

    private String doSend(String phoneNumberId, String accessToken, Map<String, Object> body) {
        try {
            JsonNode resp = rest.post()
                    .uri("/{phoneId}/messages?access_token={token}", phoneNumberId, accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (resp == null) throw new MetaApiException("Meta retornou resposta vazia");
            JsonNode messages = resp.path("messages");
            if (messages.isArray() && !messages.isEmpty()) {
                return messages.get(0).path("id").asText();
            }
            throw new MetaApiException("Meta não retornou wamid");
        } catch (HttpClientErrorException e) {
            throw new MetaApiException("Erro ao enviar mensagem WA: " + e.getResponseBodyAsString(), e);
        }
    }
}
