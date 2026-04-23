package com.erpcorporativo.web.viewmodel.usuario;

import com.erpcorporativo.api.domain.usuario.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioForm {

    @NotBlank(message = "Informe o nome.")
    @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres.")
    private String nome;

    @NotBlank(message = "Informe o e-mail.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 160, message = "E-mail deve ter no máximo 160 caracteres.")
    private String email;

    @NotNull(message = "Selecione o perfil.")
    private UserRole role;

    private boolean ativo = true;

    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres.")
    private String senha;

}
