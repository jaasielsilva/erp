package com.erpcorporativo.api.service.mail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Renova o access token do Google a partir de refresh token + client id/secret.
 */
@Service
public class GmailOauth2AccessTokenService {

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

    public String getAccessToken(String clientId, String clientSecret, String refreshToken) {
        String form =
                "grant_type="
                        + URLEncoder.encode("refresh_token", StandardCharsets.UTF_8)
                        + "&refresh_token="
                        + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
                        + "&client_id="
                        + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                        + "&client_secret="
                        + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

        try {
            String body = restClient
                    .post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isEmpty()) {
                throw new IllegalStateException("Resposta vazia do token endpoint.");
            }
            JsonNode n = objectMapper.readTree(body);
            if (n.hasNonNull("error")) {
                String desc = n.path("error_description").asText("sem detalhe");
                throw new IllegalStateException("Erro Google OAuth2: " + n.get("error").asText() + " — " + desc);
            }
            String access = n.path("access_token").asText(null);
            if (access == null || access.isEmpty()) {
                throw new IllegalStateException("Resposta de token sem access_token.");
            }
            return access;
        } catch (RestClientException e) {
            throw new IllegalStateException("Falha de rede ao renovar access token: " + e.getMessage(), e);
        } catch (Exception e) {
            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }
            throw new IllegalStateException("Falha ao processar token OAuth2.", e);
        }
    }
}
