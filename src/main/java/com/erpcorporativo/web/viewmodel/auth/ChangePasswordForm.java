package com.erpcorporativo.web.viewmodel.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordForm {

    @NotBlank(message = "Informe a senha atual.")
    private String currentPassword;

    @NotBlank(message = "Informe a nova senha.")
    @Size(min = 6, max = 120, message = "A nova senha deve ter entre 6 e 120 caracteres.")
    private String newPassword;

    @NotBlank(message = "Confirme a nova senha.")
    private String confirmPassword;
}
