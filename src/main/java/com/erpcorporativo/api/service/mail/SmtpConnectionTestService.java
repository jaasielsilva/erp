package com.erpcorporativo.api.service.mail;

import com.erpcorporativo.api.domain.configuracao.ConfiguracaoEmailEmpresa;
import com.erpcorporativo.api.repository.configuracao.ConfiguracaoEmailEmpresaRepository;
import com.erpcorporativo.api.service.configuracao.CredentialEncryptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

/**
 * Envia e-mail de teste com SMTP usando os dados persistidos do tenant.
 */
@Service
@RequiredArgsConstructor
public class SmtpConnectionTestService {

    private final ConfiguracaoEmailEmpresaRepository configuracaoEmailEmpresaRepository;
    private final CredentialEncryptionService credentialEncryptionService;
    private final SmtpMailSendService smtpMailSendService;

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

        if (StringUtils.hasText(row.getProvedor()) && !"SMTP_GMAIL".equalsIgnoreCase(row.getProvedor())) {
            throw new IllegalArgumentException("Provedor nao suportado para teste: " + row.getProvedor());
        }
        if (!StringUtils.hasText(row.getSmtpHost())
                || row.getSmtpPort() == null
                || !StringUtils.hasText(row.getSmtpUsername())
                || !StringUtils.hasText(row.getEnderecoRemetente())) {
            throw new IllegalArgumentException("Host, porta, usuario SMTP e e-mail remetente sao obrigatorios.");
        }
        if (!StringUtils.hasText(row.getSmtpPasswordCiphertext())) {
            throw new IllegalArgumentException("Senha SMTP (salva) e necessaria. Salve a configuracao.");
        }

        String smtpPassword = credentialEncryptionService.decrypt(row.getSmtpPasswordCiphertext());
        String from = row.getEnderecoRemetente().trim();
        toEmail = toEmail.trim();

        String text = "Envio de teste do ERP.\n"
                + "Se voce leu isto, o SMTP Gmail com senha de app esta configurado corretamente.\n";
        String html = "<!DOCTYPE html><html><body style=\"font-family: system-ui, sans-serif;\">"
                + "<h2 style=\"color:#0f172a;\">Conexao SMTP OK</h2>"
                + "<p>Este e um <strong>envio de teste</strong> do sistema ERP usando SMTP Gmail.</p>"
                + "<p class=\"text-muted small\">Conta remetente: " + HtmlUtils.htmlEscape(from) + "</p>"
                + "<p class=\"text-muted small\">destinatario: " + HtmlUtils.htmlEscape(toEmail) + "</p>"
                + "</body></html>";

        smtpMailSendService.send(
                row.getSmtpHost().trim(),
                row.getSmtpPort(),
                row.isSmtpAuth(),
                row.isSmtpStarttls(),
                row.getSmtpUsername().trim(),
                smtpPassword,
                from,
                toEmail,
                "Teste de conexao SMTP - ERP",
                text,
                html);
    }
}
