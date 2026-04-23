package com.erpcorporativo.api.service.mail.event;

import com.erpcorporativo.api.config.messaging.RabbitEmailProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetMailPublisher {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetMailPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final RabbitEmailProperties rabbitEmailProperties;

    public void publish(PasswordResetMailMessage message) {
        rabbitTemplate.convertAndSend(
                rabbitEmailProperties.getExchange(),
                rabbitEmailProperties.getRoutingKey(),
                message);
        log.info("Evento de e-mail de reset publicado para fila. empresaId={}, to={}", message.empresaId(), message.userEmail());
    }
}
