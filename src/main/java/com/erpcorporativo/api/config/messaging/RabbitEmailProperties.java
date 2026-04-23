package com.erpcorporativo.api.config.messaging;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.messaging.email")
public class RabbitEmailProperties {

    private String exchange = "erp.email.exchange";
    private String routingKey = "email.password-reset";
    private String queue = "erp.email.password-reset.queue";
    private String dlq = "erp.email.password-reset.dlq";
    private int maxAttempts = 4;
    private long initialIntervalMs = 2000L;
    private double multiplier = 2.0d;
    private long maxIntervalMs = 15000L;
}
