package com.erpcorporativo.api.dto.usuario;

/**
 * Resposta do reset por administrador.
 * <p>
 * Em produção, {@code temporaryPassword} e {@code resetToken} podem ser omitidos da API/UI;
 * {@code clientNotice} descreve o motivo ou próximos passos.
 */
public record UsuarioResetPasswordResponse(
        Long usuarioId,
        String temporaryPassword,
        String resetToken,
        String clientNotice
) {
    public UsuarioResetPasswordResponse(Long usuarioId, String temporaryPassword, String resetToken) {
        this(usuarioId, temporaryPassword, resetToken, null);
    }

    public static UsuarioResetPasswordResponse concealed(Long usuarioId, String clientNotice) {
        return new UsuarioResetPasswordResponse(usuarioId, null, null, clientNotice);
    }
}
