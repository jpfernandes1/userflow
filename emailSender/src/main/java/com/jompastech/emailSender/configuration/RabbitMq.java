package com.jompastech.emailSender.configuration;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMq {

    // Since we can have multiple queues, we need to name each one of them.
    // 1ª queue
    private final String queueName = "email-queue";

    @Bean
    public Queue queue(){
        return new Queue(queueName, true);
    }
}
