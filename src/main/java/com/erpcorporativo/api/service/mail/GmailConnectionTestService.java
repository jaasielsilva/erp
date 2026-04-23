package com.erpcorporativo.api.service.mail;

import com.erpcorporativo.api.domain.configuracao.ConfiguracaoEmailEmpresa;
import com.erpcorporativo.api.repository.configuracao.ConfiguracaoEmailEmpresaRepository;
import com.erpcorporativo.api.service.configuracao.CredentialEncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

/**
 * Envia um e-mail de prova para validar OAuth, Gmail API e credenciais cifradas.
 * Usa apenas o que está persistido no banco (salve a configuração antes de testar).
 */
@Service
@RequiredArgsConstructor
public class GmailConnectionTestService {

    private final ConfiguracaoEmailEmpresaRepository configuracaoEmailEmpresaRepository;
    private final CredentialEncryptionService credentialEncryptionService;
    private final GmailOauth2AccessTokenService gmailOauth2AccessTokenService;
    private final GmailApiSendService gmailApiSendService;

    public void sendTestEmail(Long empresaId, String toEmail) {
        if (empresaId == null) {
            throw new IllegalArgumentException("Empresa nao informada.");
        }
        if (!StringUtils.hasText(toEmail) || !toEmail.contains("@")) {
            throw new IllegalArgumentException("Informe um e-mail de destino valido para o teste.");
        }

        ConfiguracaoEmailEmpresa row = configuracaoEmailEmpresaRepository
                .findByEmpresa_Id(empresaId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nenhuma configuracao salva para este tenant. Preencha e salve antes de testar."));

        if (StringUtils.hasText(row.getProvedor()) && !"GMAIL_OAUTH2".equalsIgnoreCase(row.getProvedor())) {
            throw new IllegalArgumentException("Provedor nao suportado para teste: " + row.getProvedor());
        }
        if (!StringUtils.hasText(row.getOauthClientId()) || !StringUtils.hasText(row.getEnderecoRemetente())) {
            throw new IllegalArgumentException("Client ID e e-mail remetente sao obrigatorios. Salve a configuracao.");
        }
        if (!StringUtils.hasText(row.getOauthClientSecretCiphertext()) || !StringUtils.hasText(row.getOauthRefreshTokenCiphertext())) {
            throw new IllegalArgumentException("Client secret e refresh token (salvos) sao necessarios. Salve a configuracao.");
        }

        String clientSecret = credentialEncryptionService.decrypt(row.getOauthClientSecretCiphertext());
        String refresh = credentialEncryptionService.decrypt(row.getOauthRefreshTokenCiphertext());
        String access = gmailOauth2AccessTokenService.getAccessToken(row.getOauthClientId(), clientSecret, refresh);
        String from = row.getEnderecoRemetente().trim();
        toEmail = toEmail.trim();

        String text = "Envio de teste do ERP.\n"
                + "Se voce leu isto, o OAuth, Gmail API e a cifra estao coerentes com a conta remetente.\n";
        String html = "<!DOCTYPE html><html><body style=\"font-family: system-ui, sans-serif;\">"
                + "<h2 style=\"color:#0f172a;\">Conexao OK</h2>"
                + "<p>Este e um <strong>envio de teste</strong> do sistema ERP. "
                + "A renovacao do token e o envio via Gmail API funcionaram.</p>"
                + "<p class=\"text-muted small\">Conta remetente: " + HtmlUtils.htmlEscape(from) + "</p>"
                + "<p class=\"text-muted small\">destinatario: " + HtmlUtils.htmlEscape(toEmail) + "</p>"
                + "</body></html>";

        gmailApiSendService.send(
                access,
                from,
                toEmail,
                "Teste de conexao - ERP",
                text,
                html);
    }
}
