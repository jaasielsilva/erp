package com.erpcorporativo.web.viewmodel.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordByTokenForm {

    @NotBlank
    private String token;

    @NotBlank(message = "Informe a nova senha.")
    @Size(min = 8, max = 72, message = "A nova senha deve ter entre 8 e 72 caracteres.")
    private String newPassword;

    @NotBlank(message = "Confirme a nova senha.")
    private String confirmPassword;
}
