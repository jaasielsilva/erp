package com.erpcorporativo.web.viewmodel.configuracao;

import lombok.Data;

@Data
public class EmailConfigForm {

    private boolean ativo;
    private String enderecoRemetente;
    private String smtpHost;
    private Integer smtpPort;
    private boolean smtpAuth = true;
    private boolean smtpStarttls = true;
    private String smtpUsername;
    /** Vazio = manter valor já salvo (se existir). */
    private String smtpPassword;
}
