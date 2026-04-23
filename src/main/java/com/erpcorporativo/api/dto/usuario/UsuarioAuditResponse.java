package com.erpcorporativo.api.dto.usuario;

import com.erpcorporativo.api.domain.usuario.UsuarioAuditAction;
import java.time.Instant;

public record UsuarioAuditResponse(
        UsuarioAuditAction action,
        String performedBy,
        String details,
        Instant createdAt
) {
}
