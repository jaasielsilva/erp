package com.erpcorporativo.web.viewmodel.empresa;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmpresaForm {

    @NotBlank(message = "Informe o nome fantasia da empresa.")
    @Size(max = 200, message = "Nome fantasia deve ter no maximo 200 caracteres.")
    private String nomeFantasia;

    private boolean ativo = true;

    @NotBlank(message = "Informe o nome do administrador inicial.")
    @Size(max = 120, message = "Nome do administrador deve ter no maximo 120 caracteres.")
    private String adminNome;

    @NotBlank(message = "Informe o e-mail do administrador inicial.")
    @Email(message = "Informe um e-mail valido.")
    @Size(max = 160, message = "E-mail deve ter no maximo 160 caracteres.")
    private String adminEmail;

    @NotBlank(message = "Informe a senha inicial do administrador.")
    @Size(min = 8, max = 120, message = "Senha inicial deve ter entre 8 e 120 caracteres.")
    private String adminSenha;
}
