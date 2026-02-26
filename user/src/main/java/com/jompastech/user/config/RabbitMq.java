package com.jompastech.user.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.MessageConverter;

@Configuration
public class
RabbitMq {

    public static final String EXCHANGE = "email-exchange";
    public static final String ROUTING_KEY = "email-routing-key";

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(
            ConnectionFactory connectionFactory,
            MessageConverter messageConverter) {

        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);

        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                System.out.println("Message confirmed: " +
                        (correlationData != null ? correlationData.getId() : "null"));
            } else {
                System.err.println("Message NOT delivered: " + cause);
            }
        });
        template.setReturnsCallback(returned ->
                System.err.println("Message returned: " + returned.getMessage())
        );
        return template;
    }
}