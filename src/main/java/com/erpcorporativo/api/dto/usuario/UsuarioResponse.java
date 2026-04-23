package com.erpcorporativo.api.dto.usuario;

import com.erpcorporativo.api.domain.usuario.UserRole;
import java.time.Instant;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        UserRole role,
        boolean ativo,
        String createdBy,
        String updatedBy,
        Instant createdAt,
        Instant updatedAt
) {
}
