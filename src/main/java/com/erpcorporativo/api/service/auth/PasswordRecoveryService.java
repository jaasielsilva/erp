package com.erpcorporativo.api.service.auth;

import com.erpcorporativo.api.domain.usuario.PasswordResetToken;
import com.erpcorporativo.api.domain.usuario.Usuario;
import com.erpcorporativo.api.domain.usuario.UsuarioAuditAction;
import com.erpcorporativo.api.domain.usuario.UsuarioAuditLog;
import com.erpcorporativo.api.repository.usuario.PasswordResetTokenRepository;
import com.erpcorporativo.api.repository.usuario.UsuarioAuditLogRepository;
import com.erpcorporativo.api.repository.usuario.UsuarioRepository;
import com.erpcorporativo.api.service.mail.event.PasswordResetMailMessage;
import com.erpcorporativo.api.service.mail.event.PasswordResetMailPublisher;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(PasswordRecoveryService.class);
    private static final String TOKEN_CREATED_BY_SELF_SERVICE = "self-service";

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UsuarioAuditLogRepository usuarioAuditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailPublisher passwordResetMailPublisher;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public void requestPasswordReset(String email) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByEmailIgnoreCaseAndAtivoTrue(normalizedEmail).orElse(null);
        if (usuario == null) {
            return; // resposta neutra contra enumeracao de usuarios
        }
        if (usuario.getEmpresaId() == null) {
            log.warn("Recuperacao ignorada para usuario sem tenant: {}", normalizedEmail);
            return;
        }

        passwordResetTokenRepository.deleteByUsuarioId(usuario.getId());

        String rawToken = generateSecureToken();
        PasswordResetToken token = new PasswordResetToken();
        token.setUsuarioId(usuario.getId());
        token.setToken(rawToken);
        token.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        token.setCreatedBy(TOKEN_CREATED_BY_SELF_SERVICE);
        passwordResetTokenRepository.save(token);

        final Long empresaId = usuario.getEmpresaId();
        final String to = usuario.getEmail();
        final String name = usuario.getNome();
        final String tokenValue = rawToken;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    passwordResetMailPublisher.publish(
                            PasswordResetMailMessage.forForgotPasswordLink(empresaId, to, name, tokenValue));
                } catch (Exception ex) {
                    log.warn("Falha ao publicar recuperacao de senha para fila: {}", ex.getMessage());
                }
            }
        });
    }

    @Transactional(readOnly = true)
    public boolean isTokenValid(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        return passwordResetTokenRepository.findByTokenAndUsedAtIsNullAndExpiresAtAfterAndCreatedBy(
                token.trim(),
                Instant.now(),
                TOKEN_CREATED_BY_SELF_SERVICE).isPresent();
    }

    @Transactional
    public void resetPasswordWithToken(String token, String newPassword, String confirmPassword) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Link de redefinicao invalido ou expirado.");
        }
        validatePassword(newPassword, confirmPassword);

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenAndUsedAtIsNullAndExpiresAtAfterAndCreatedBy(
                        token.trim(),
                        Instant.now(),
                        TOKEN_CREATED_BY_SELF_SERVICE)
                .orElseThrow(() -> new IllegalArgumentException("Link de redefinicao invalido ou expirado."));

        Usuario usuario = usuarioRepository.findById(resetToken.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario vinculado ao token nao encontrado."));
        if (!usuario.isAtivo()) {
            throw new IllegalArgumentException("Conta inativa. Contate um administrador.");
        }

        usuario.setSenhaHash(passwordEncoder.encode(newPassword));
        usuario.setMustChangePassword(false);
        usuario.setUpdatedBy(TOKEN_CREATED_BY_SELF_SERVICE);
        usuarioRepository.save(usuario);

        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);
        passwordResetTokenRepository.deleteByUsuarioId(usuario.getId());

        UsuarioAuditLog logEntry = new UsuarioAuditLog();
        logEntry.setUsuarioId(usuario.getId());
        logEntry.setAction(UsuarioAuditAction.CHANGE_OWN_PASSWORD);
        logEntry.setPerformedBy(TOKEN_CREATED_BY_SELF_SERVICE);
        logEntry.setDetails("Senha redefinida via fluxo de esqueci minha senha.");
        usuarioAuditLogRepository.save(logEntry);
    }

    private void validatePassword(String newPassword, String confirmPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 8) {
            throw new IllegalArgumentException("A nova senha deve ter no minimo 8 caracteres.");
        }
        if (newPassword.length() > 72) {
            throw new IllegalArgumentException("A nova senha deve ter no maximo 72 caracteres.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("A confirmacao da senha nao confere.");
        }
        boolean hasUpper = newPassword.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = newPassword.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = newPassword.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = newPassword.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new IllegalArgumentException(
                    "A senha deve conter letra maiuscula, minuscula, numero e caractere especial.");
        }
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
