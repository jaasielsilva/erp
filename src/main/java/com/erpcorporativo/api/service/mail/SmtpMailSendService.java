package com.erpcorporativo.api.service.mail;

import jakarta.activation.DataHandler;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SmtpMailSendService {

    public void send(
            String smtpHost,
            int smtpPort,
            boolean smtpAuth,
            boolean smtpStarttls,
            String smtpUsername,
            String smtpPassword,
            String from,
            String to,
            String subject,
            String textBody,
            String htmlBody) {
        if (!StringUtils.hasText(smtpHost) || smtpPort <= 0) {
            throw new IllegalArgumentException("Host e porta SMTP sao obrigatorios.");
        }
        if (!StringUtils.hasText(from) || !StringUtils.hasText(to)) {
            throw new IllegalArgumentException("from e to sao obrigatorios.");
        }
        if (smtpAuth && (!StringUtils.hasText(smtpUsername) || !StringUtils.hasText(smtpPassword))) {
            throw new IllegalArgumentException("Usuario e senha SMTP sao obrigatorios quando auth=true.");
        }

        try {
            boolean useStarttls = smtpStarttls || smtpPort == 587;
            boolean useAuth = smtpAuth || smtpPort == 587;

            Properties props = new Properties();
            props.put("mail.smtp.host", smtpHost);
            props.put("mail.smtp.port", String.valueOf(smtpPort));
            props.put("mail.smtp.auth", String.valueOf(useAuth));
            props.put("mail.smtp.starttls.enable", String.valueOf(useStarttls));
            props.put("mail.smtp.starttls.required", String.valueOf(useStarttls));
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "15000");
            props.put("mail.smtp.writetimeout", "15000");

            Session session;
            if (useAuth) {
                session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(smtpUsername, smtpPassword);
                    }
                });
            } else {
                session = Session.getInstance(props);
            }

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
            message.setSubject(subject, StandardCharsets.UTF_8.name());

            String plain = StringUtils.hasText(textBody) ? textBody : stripHtmlSimple(htmlBody);
            if (!StringUtils.hasText(plain) && StringUtils.hasText(htmlBody)) {
                plain = "Visualize o HTML deste e-mail no cliente.";
            }
            if (StringUtils.hasText(htmlBody)) {
                MimeMultipart alternative = new MimeMultipart("alternative");
                MimeBodyPart textPart = new MimeBodyPart();
                textPart.setDataHandler(new DataHandler(new ByteArrayDataSource(plain, "text/plain; charset=UTF-8")));
                alternative.addBodyPart(textPart);
                MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setDataHandler(new DataHandler(new ByteArrayDataSource(htmlBody, "text/html; charset=UTF-8")));
                alternative.addBodyPart(htmlPart);
                message.setContent(alternative);
            } else {
                message.setText(plain, StandardCharsets.UTF_8.name());
            }
            Transport.send(message);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao enviar e-mail via SMTP: " + e.getMessage(), e);
        }
    }

    private static String stripHtmlSimple(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }
}
