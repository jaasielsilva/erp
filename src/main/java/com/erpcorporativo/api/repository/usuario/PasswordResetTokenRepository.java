package com.erpcorporativo.api.repository.usuario;

import com.erpcorporativo.api.domain.usuario.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    void deleteByUsuarioId(Long usuarioId);

    Optional<PasswordResetToken> findByTokenAndUsedAtIsNull(String token);

    Optional<PasswordResetToken> findByTokenAndUsedAtIsNullAndExpiresAtAfterAndCreatedBy(
            String token, Instant now, String createdBy);
}
