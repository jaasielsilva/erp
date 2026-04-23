package com.erpcorporativo.api.service.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Envia e-mail via Gmail API (users.me.messages.send) usando access token OAuth2.
 * Evita a complexidade do SMTP com XOAUTH2.
 */
@Service
public class GmailApiSendService {

    private static final String SEND_BASE = "https://gmail.googleapis.com/gmail/v1/users/";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create();

    public void send(String accessToken, String from, String to, String subject, String textBody, String htmlBody) {
        if (!StringUtils.hasText(from) || !StringUtils.hasText(to) || !StringUtils.hasText(accessToken)) {
            throw new IllegalArgumentException("from, to e accessToken sao obrigatorios.");
        }
        try {
            String rawRfc = buildMimeRaw(from, to, subject, textBody, htmlBody);
            String urlSafeB64 = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(rawRfc.getBytes(StandardCharsets.UTF_8));

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("raw", urlSafeB64);

            String url = SEND_BASE + "me/messages/send";
            String json = objectMapper.writeValueAsString(payload);
            String responseBody = restClient
                    .post()
                    .uri(url)
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(json)
                    .retrieve()
                    .body(String.class);
            if (responseBody != null && responseBody.contains("\"error\"")) {
                throw new IllegalStateException("Gmail API retornou erro: " + responseBody);
            }
        } catch (RestClientException e) {
            throw new IllegalStateException("Falha de rede no envio pelo Gmail: " + e.getMessage(), e);
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao enviar e-mail via Gmail API.", e);
        }
    }

    private static String buildMimeRaw(String from, String to, String subject, String textBody, String htmlBody) throws Exception {
        Properties p = new Properties();
        Session session = Session.getInstance(p, null);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
        message.setSubject(subject, StandardCharsets.UTF_8.name());

        String plain = StringUtils.hasText(textBody) ? textBody : stripHtmlSimple(htmlBody);
        if (!StringUtils.hasText(plain) && StringUtils.hasText(htmlBody)) {
            plain = "Visualize o HTML deste e-mail no cliente.";
        }
        if (StringUtils.hasText(htmlBody)) {
            var alternative = new MimeMultipart("alternative");
            var textPart = new MimeBodyPart();
            textPart.setDataHandler(
                    new DataHandler(new ByteArrayDataSource(plain, "text/plain; charset=UTF-8")));
            alternative.addBodyPart(textPart);
            var htmlPart = new MimeBodyPart();
            htmlPart.setDataHandler(
                    new DataHandler(new ByteArrayDataSource(htmlBody, "text/html; charset=UTF-8")));
            alternative.addBodyPart(htmlPart);
            message.setContent(alternative);
        } else {
            message.setText(plain, StandardCharsets.UTF_8.name());
        }

        var out = new ByteArrayOutputStream();
        message.writeTo(out);
        return out.toString(StandardCharsets.UTF_8);
    }

    private static String stripHtmlSimple(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }
}
