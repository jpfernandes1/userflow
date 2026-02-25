package com.jompastech.user.service;

import com.jompastech.user.config.RabbitMq;
import com.jompastech.user.model.dto.EmailEventDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class EmailPublisher {

    private final RabbitTemplate rabbitTemplate;

    public EmailPublisher(RabbitTemplate rabbitTemplate){
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(EmailEventDTO event){
        rabbitTemplate.convertAndSend(
                RabbitMq.EXCHANGE,
                RabbitMq.ROUTING_KEY,
                event
        );
    }
}
