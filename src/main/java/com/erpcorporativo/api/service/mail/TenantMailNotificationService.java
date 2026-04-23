package com.erpcorporativo.api.service.mail;

import com.erpcorporativo.api.domain.configuracao.ConfiguracaoEmailEmpresa;
import com.erpcorporativo.api.repository.configuracao.ConfiguracaoEmailEmpresaRepository;
import com.erpcorporativo.api.service.configuracao.CredentialEncryptionService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

/**
 * Orquestra envio de notificações por tenant usando SMTP (Gmail + senha de app).
 */
@Service
@RequiredArgsConstructor
public class TenantMailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TenantMailNotificationService.class);

    private final ConfiguracaoEmailEmpresaRepository configuracaoEmailEmpresaRepository;
    private final CredentialEncryptionService credentialEncryptionService;
    private final SmtpMailSendService smtpMailSendService;

    @Value("${app.mail.send-on-admin-password-reset:true}")
    private boolean sendOnAdminPasswordReset;

    @Value("${app.mail.include-temporary-password-in-body:false}")
    private boolean includeTemporaryPasswordInBody;

    @Value("${app.web.public-base-url:}")
    private String publicBaseUrl;

    /**
     * Best-effort: não propaga exceção (evita derrubar transação; erros vão no log).
     */
    public void trySendPasswordResetByAdmin(
            Long empresaId,
            String userEmail,
            String userName,
            String temporaryPassword) {
        try {
            sendPasswordResetByAdminOrThrow(empresaId, userEmail, userName, temporaryPassword);
        } catch (Exception e) {
            log.warn("Falha ao enviar e-mail de redefinicao de senha para {}: {}", userEmail, e.getMessage());
        }
    }

    /**
     * Modo estrito: propaga falhas para permitir retry/DLQ em processamento assíncrono.
     */
    public void sendPasswordResetByAdminOrThrow(
            Long empresaId,
            String userEmail,
            String userName,
            String temporaryPassword) {
        if (!sendOnAdminPasswordReset) {
            return;
        }
        if (empresaId == null) {
            throw new IllegalArgumentException("Destinatario sem empresa (empresa_id nulo).");
        }
        ConfiguracaoEmailEmpresa row = configuracaoEmailEmpresaRepository
                .findByEmpresa_Id(empresaId)
                .orElseThrow(() -> new IllegalStateException("Config de e-mail inativa/ausente para empresa " + empresaId + "."));
        if (!row.isAtivo()) {
            throw new IllegalStateException("Config de e-mail inativa para empresa " + empresaId + ".");
        }
        if (StringUtils.hasText(row.getProvedor()) && !"SMTP_GMAIL".equalsIgnoreCase(row.getProvedor())) {
            throw new IllegalStateException("Provedor de e-mail nao suportado: " + row.getProvedor());
        }
        if (!StringUtils.hasText(row.getSmtpHost())
                || row.getSmtpPort() == null
                || !StringUtils.hasText(row.getSmtpUsername())
                || !StringUtils.hasText(row.getEnderecoRemetente())) {
            throw new IllegalStateException("Parametros SMTP ou remetente ausentes para empresa " + empresaId + ".");
        }
        if (!StringUtils.hasText(row.getSmtpPasswordCiphertext())) {
            throw new IllegalStateException("Senha SMTP nao cadastrada para empresa " + empresaId + ".");
        }

        String smtpPassword = credentialEncryptionService.decrypt(row.getSmtpPasswordCiphertext());

        String nameSafe = HtmlUtils.htmlEscape(userName == null ? "" : userName);
        String linkHtml = buildLoginLineHtml();
        String loginText = buildLoginLineText();
        String bodyHtml = buildPasswordResetBodyHtml(
                nameSafe, temporaryPassword, includeTemporaryPasswordInBody, linkHtml);
        String bodyText = buildPasswordResetBodyText(
                userName, temporaryPassword, includeTemporaryPasswordInBody, loginText);

        smtpMailSendService.send(
                row.getSmtpHost().trim(),
                row.getSmtpPort(),
                row.isSmtpAuth(),
                row.isSmtpStarttls(),
                row.getSmtpUsername().trim(),
                smtpPassword,
                row.getEnderecoRemetente().trim(),
                userEmail,
                "Senha redefinida - ERP",
                bodyText,
                bodyHtml);
    }

    /**
     * Envia link seguro de recuperacao de senha para fluxo "esqueci minha senha".
     */
    public void sendForgotPasswordLinkOrThrow(
            Long empresaId,
            String userEmail,
            String userName,
            String resetToken) {
        if (!StringUtils.hasText(resetToken)) {
            throw new IllegalArgumentException("Token de recuperacao ausente.");
        }
        if (empresaId == null) {
            throw new IllegalArgumentException("Destinatario sem empresa (empresa_id nulo).");
        }
        ConfiguracaoEmailEmpresa row = configuracaoEmailEmpresaRepository
                .findByEmpresa_Id(empresaId)
                .orElseThrow(() -> new IllegalStateException("Config de e-mail inativa/ausente para empresa " + empresaId + "."));
        if (!row.isAtivo()) {
            throw new IllegalStateException("Config de e-mail inativa para empresa " + empresaId + ".");
        }
        if (StringUtils.hasText(row.getProvedor()) && !"SMTP_GMAIL".equalsIgnoreCase(row.getProvedor())) {
            throw new IllegalStateException("Provedor de e-mail nao suportado: " + row.getProvedor());
        }
        if (!StringUtils.hasText(row.getSmtpHost())
                || row.getSmtpPort() == null
                || !StringUtils.hasText(row.getSmtpUsername())
                || !StringUtils.hasText(row.getEnderecoRemetente())) {
            throw new IllegalStateException("Parametros SMTP ou remetente ausentes para empresa " + empresaId + ".");
        }
        if (!StringUtils.hasText(row.getSmtpPasswordCiphertext())) {
            throw new IllegalStateException("Senha SMTP nao cadastrada para empresa " + empresaId + ".");
        }
        String resetUrl = buildForgotPasswordUrl(resetToken);
        if (!StringUtils.hasText(resetUrl)) {
            throw new IllegalStateException(
                    "Defina app.web.public-base-url para enviar links de recuperacao por e-mail.");
        }

        String smtpPassword = credentialEncryptionService.decrypt(row.getSmtpPasswordCiphertext());
        String nameSafe = HtmlUtils.htmlEscape(userName == null ? "" : userName);

        String bodyHtml = "<!DOCTYPE html><html><body style=\"font-family: system-ui, sans-serif; color:#0f172a;\">"
                + "<p>Ola, " + nameSafe + ",</p>"
                + "<p>Recebemos uma solicitacao para redefinir sua senha no ERP.</p>"
                + "<p><a href=\"" + HtmlUtils.htmlEscape(resetUrl) + "\" "
                + "style=\"display:inline-block;padding:10px 14px;background:#2563eb;color:#fff;border-radius:8px;text-decoration:none;\">"
                + "Redefinir minha senha</a></p>"
                + "<p>Se o botao nao funcionar, copie e cole o link abaixo:</p>"
                + "<p><a href=\"" + HtmlUtils.htmlEscape(resetUrl) + "\">" + HtmlUtils.htmlEscape(resetUrl) + "</a></p>"
                + "<p>Este link expira em 30 minutos e pode ser usado apenas uma vez.</p>"
                + "<p class=\"text-muted small\">Se voce nao solicitou, ignore esta mensagem.</p>"
                + "</body></html>";

        String bodyText = "Ola, " + (StringUtils.hasText(userName) ? userName : "Usuario") + ",\n\n"
                + "Recebemos uma solicitacao para redefinir sua senha no ERP.\n"
                + "Use o link abaixo (valido por 30 minutos):\n"
                + resetUrl + "\n\n"
                + "Se voce nao solicitou, ignore esta mensagem.\n";

        smtpMailSendService.send(
                row.getSmtpHost().trim(),
                row.getSmtpPort(),
                row.isSmtpAuth(),
                row.isSmtpStarttls(),
                row.getSmtpUsername().trim(),
                smtpPassword,
                row.getEnderecoRemetente().trim(),
                userEmail,
                "Recuperacao de senha - ERP",
                bodyText,
                bodyHtml);
    }

    private String buildLoginLineHtml() {
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        if (!StringUtils.hasText(base)) {
            return "";
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return "<p class=\"mb-0\">Acesse o sistema: <a href=\"" + HtmlUtils.htmlEscape(base + "/login")
                + "\">" + HtmlUtils.htmlEscape(base + "/login") + "</a></p>";
    }

    private String buildLoginLineText() {
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        if (!StringUtils.hasText(base)) {
            return "";
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return "Acesse o sistema: " + base + "/login\n";
    }

    private String buildForgotPasswordUrl(String resetToken) {
        String base = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        if (!StringUtils.hasText(base)) {
            return "";
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/redefinir-senha?token="
                + URLEncoder.encode(resetToken, StandardCharsets.UTF_8);
    }

    private static String buildPasswordResetBodyHtml(
            String nameHtmlEscaped,
            String temporaryPassword,
            boolean includeTemp,
            String loginBlockHtml) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><body style=\"font-family: system-ui, sans-serif; color:#0f172a;\">");
        sb.append("<p>Ol&aacute;, ").append(nameHtmlEscaped).append(",</p>");
        sb.append("<p>Sua senha de acesso foi redefinida por um <strong>administrador</strong>. ");
        sb.append("Ao entrar, ser&aacute; necess&aacute;rio troc&aacute;-la, se ainda n&atilde;o tiver feito isso.</p>");
        if (includeTemp && StringUtils.hasText(temporaryPassword)) {
            sb.append("<p><strong>Senha tempor&aacute;ria (guarde com seguran&ccedil;a e troque ap&oacute;s o acesso):</strong> <code>");
            sb.append(HtmlUtils.htmlEscape(temporaryPassword));
            sb.append("</code></p>");
        } else {
            sb.append(
                    "<p>Utilize a senha tempor&aacute;ria que o administrador lhe forneceu. Por seguran&ccedil;a, ela n&atilde;o &eacute; inclu&iacute;da automaticamente neste e-mail.</p>");
        }
        if (StringUtils.hasText(loginBlockHtml)) {
            sb.append(loginBlockHtml);
        }
        sb.append("<p class=\"text-muted small\">Mensagem autom&aacute;tica. N&atilde;o responda.</p></body></html>");
        return sb.toString();
    }

    private static String buildPasswordResetBodyText(
            String userName, String temporaryPassword, boolean includeTemp, String loginLine) {
        String n = StringUtils.hasText(userName) ? userName : "Usuario";
        StringBuilder sb = new StringBuilder();
        sb.append("Ola, ").append(n).append(",\n\n");
        sb.append("Sua senha de acesso foi redefinida por um administrador.\n");
        if (includeTemp && StringUtils.hasText(temporaryPassword)) {
            sb.append("Senha temporaria: ").append(temporaryPassword).append("\n");
        } else {
            sb.append("Use a senha temporaria que o administrador lhe forneceu (nao reenviada neste e-mail).\n");
        }
        if (StringUtils.hasText(loginLine)) {
            sb.append("\n").append(loginLine);
        }
        return sb.toString();
    }
}
