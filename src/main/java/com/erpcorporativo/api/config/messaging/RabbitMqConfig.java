package com.erpcorporativo.api.config.messaging;

import java.util.Map;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RabbitEmailProperties.class)
public class RabbitMqConfig {

    @Bean
    Declarables emailDeclarables(RabbitEmailProperties props) {
        DirectExchange exchange = new DirectExchange(props.getExchange(), true, false);
        Queue queue = new Queue(
                props.getQueue(),
                true,
                false,
                false,
                Map.of("x-dead-letter-exchange", props.getExchange(), "x-dead-letter-routing-key", props.getDlq()));
        Queue dlq = new Queue(props.getDlq(), true);
        Binding binding = BindingBuilder.bind(queue).to(exchange).with(props.getRoutingKey());
        Binding dlqBinding = BindingBuilder.bind(dlq).to(exchange).with(props.getDlq());
        return new Declarables(exchange, queue, dlq, binding, dlqBinding);
    }

    @Bean
    MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(rabbitMessageConverter);
        return template;
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter rabbitMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        factory.setDefaultRequeueRejected(false); // mensagens rejeitadas vao para DLQ
        return factory;
    }
}
