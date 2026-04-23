package com.erpcorporativo.api.dto.usuario;

import com.erpcorporativo.api.domain.usuario.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioCreateRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Email @Size(max = 160) String email,
        @NotNull UserRole role,
        boolean ativo,
        @NotBlank @Size(min = 6, max = 120) String senha
) {
}
