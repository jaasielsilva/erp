package com.erpcorporativo.api.service.mail.event;

import com.erpcorporativo.api.service.mail.TenantMailNotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetMailConsumer {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailConsumer.class);

    private final TenantMailNotificationService tenantMailNotificationService;

    @RabbitListener(queues = "${app.messaging.email.queue:erp.email.password-reset.queue}")
    public void consume(PasswordResetMailMessage message) {
        log.info("Processando e-mail de reset por fila. empresaId={}, to={}", message.empresaId(), message.userEmail());
        if (PasswordResetMailMessage.KIND_FORGOT_PASSWORD_LINK.equals(message.kind())) {
            tenantMailNotificationService.sendForgotPasswordLinkOrThrow(
                    message.empresaId(),
                    message.userEmail(),
                    message.userName(),
                    message.resetToken());
            return;
        }
        tenantMailNotificationService.sendPasswordResetByAdminOrThrow(
                message.empresaId(),
                message.userEmail(),
                message.userName(),
                message.temporaryPassword());
    }
}
