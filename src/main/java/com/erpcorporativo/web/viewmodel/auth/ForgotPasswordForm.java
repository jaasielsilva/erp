package com.erpcorporativo.web.viewmodel.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordForm {

    @NotBlank(message = "Informe seu e-mail.")
    @Email(message = "Informe um e-mail valido.")
    private String email;
}
