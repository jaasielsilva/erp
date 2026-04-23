package com.erpcorporativo.api.service.configuracao;

import com.erpcorporativo.api.domain.configuracao.ConfiguracaoEmailEmpresa;
import com.erpcorporativo.api.domain.empresa.Empresa;
import com.erpcorporativo.api.repository.configuracao.ConfiguracaoEmailEmpresaRepository;
import com.erpcorporativo.api.repository.empresa.EmpresaRepository;
import com.erpcorporativo.web.viewmodel.configuracao.EmailConfigForm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ConfiguracaoEmailService {

    private final EmpresaRepository empresaRepository;
    private final ConfiguracaoEmailEmpresaRepository configuracaoEmailEmpresaRepository;
    private final CredentialEncryptionService credentialEncryptionService;

    @Transactional(readOnly = true)
    public EmailConfigForm loadForm(Long empresaId) {
        empresaRepository.findById(empresaId).orElseThrow(() -> new IllegalArgumentException("Empresa nao encontrada."));
        ConfiguracaoEmailEmpresa row = configuracaoEmailEmpresaRepository.findByEmpresa_Id(empresaId).orElse(null);

        EmailConfigForm form = new EmailConfigForm();
        if (row == null) {
            form.setSmtpHost("smtp.gmail.com");
            form.setSmtpPort(587);
            form.setSmtpAuth(true);
            form.setSmtpStarttls(true);
            return form;
        }
        form.setAtivo(row.isAtivo());
        form.setEnderecoRemetente(row.getEnderecoRemetente());
        form.setSmtpHost(defaultIfBlank(row.getSmtpHost(), "smtp.gmail.com"));
        form.setSmtpPort(row.getSmtpPort() != null ? row.getSmtpPort() : 587);
        form.setSmtpAuth(row.isSmtpAuth());
        form.setSmtpStarttls(row.isSmtpStarttls());
        form.setSmtpUsername(row.getSmtpUsername());
        return form;
    }

    @Transactional(readOnly = true)
    public boolean hasStoredSmtpPassword(Long empresaId) {
        return configuracaoEmailEmpresaRepository
                .findByEmpresa_Id(empresaId)
                .map(c -> StringUtils.hasText(c.getSmtpPasswordCiphertext()))
                .orElse(false);
    }

    @Transactional
    public void save(Long empresaId, EmailConfigForm form) {
        Empresa empresa = empresaRepository.findById(empresaId).orElseThrow(() -> new IllegalArgumentException("Empresa nao encontrada."));

        ConfiguracaoEmailEmpresa row = configuracaoEmailEmpresaRepository
                .findByEmpresa_Id(empresaId)
                .orElseGet(() -> {
                    ConfiguracaoEmailEmpresa created = new ConfiguracaoEmailEmpresa();
                    created.setEmpresa(empresa);
                    created.setProvedor("SMTP_GMAIL");
                    created.setSmtpHost("smtp.gmail.com");
                    created.setSmtpPort(587);
                    created.setSmtpAuth(true);
                    created.setSmtpStarttls(true);
                    return created;
                });

        row.setAtivo(form.isAtivo());
        row.setEnderecoRemetente(trimToNull(form.getEnderecoRemetente()));
        row.setProvedor("SMTP_GMAIL");
        row.setSmtpHost(defaultIfBlank(trimToNull(form.getSmtpHost()), "smtp.gmail.com"));
        row.setSmtpPort(form.getSmtpPort() != null ? form.getSmtpPort() : 587);
        row.setSmtpAuth(form.isSmtpAuth());
        row.setSmtpStarttls(form.isSmtpStarttls());
        row.setSmtpUsername(trimToNull(form.getSmtpUsername()));

        // Gmail em 587 exige STARTTLS e autenticacao.
        if (row.getSmtpPort() != null && row.getSmtpPort() == 587) {
            row.setSmtpStarttls(true);
            row.setSmtpAuth(true);
        }

        if (StringUtils.hasText(form.getSmtpPassword())) {
            row.setSmtpPasswordCiphertext(credentialEncryptionService.encrypt(form.getSmtpPassword().trim()));
        }

        if (form.isAtivo()) {
            if (!StringUtils.hasText(row.getEnderecoRemetente())) {
                throw new IllegalArgumentException("Informe o e-mail remetente (conta Gmail autorizada).");
            }
            if (!StringUtils.hasText(row.getSmtpHost())) {
                throw new IllegalArgumentException("Informe o host SMTP.");
            }
            if (row.getSmtpPort() == null || row.getSmtpPort() <= 0) {
                throw new IllegalArgumentException("Informe uma porta SMTP valida.");
            }
            if (!StringUtils.hasText(row.getSmtpUsername())) {
                throw new IllegalArgumentException("Informe o usuario SMTP (Gmail completo).");
            }
            if (!StringUtils.hasText(row.getSmtpPasswordCiphertext())) {
                throw new IllegalArgumentException(
                        "Com envio ativo, e necessario ter senha de app SMTP. "
                                + "Preencha o campo de senha ou deixe em branco se ja estiver salva.");
            }
        }

        configuracaoEmailEmpresaRepository.save(row);
    }

    private static String defaultIfBlank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
