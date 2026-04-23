package com.erpcorporativo.api.service.mail.event;

public record PasswordResetMailMessage(
        String kind,
        Long empresaId,
        String userEmail,
        String userName,
        String temporaryPassword,
        String resetToken) {

    public static final String KIND_ADMIN_TEMP_PASSWORD = "ADMIN_TEMP_PASSWORD";
    public static final String KIND_FORGOT_PASSWORD_LINK = "FORGOT_PASSWORD_LINK";

    public static PasswordResetMailMessage forAdminTempPassword(
            Long empresaId, String userEmail, String userName, String temporaryPassword) {
        return new PasswordResetMailMessage(
                KIND_ADMIN_TEMP_PASSWORD, empresaId, userEmail, userName, temporaryPassword, null);
    }

    public static PasswordResetMailMessage forForgotPasswordLink(
            Long empresaId, String userEmail, String userName, String resetToken) {
        return new PasswordResetMailMessage(
                KIND_FORGOT_PASSWORD_LINK, empresaId, userEmail, userName, null, resetToken);
    }
}
