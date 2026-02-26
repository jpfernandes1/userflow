package com.jompastech.emailSender.consumer;

import com.jompastech.emailSender.configuration.RabbitMq;
import com.jompastech.emailSender.model.event.EmailEventDTO;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class EmailDeadLetterConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(EmailDeadLetterConsumer.class);

    @RabbitListener(queues = RabbitMq.DLQ)
    public void handleDeadLetter(EmailEventDTO event, Message message) {

        log.error("DLQ message received for userId: {}", event.userId());

        var headers = message.getMessageProperties().getHeaders();
        log.error("x-death: {}", headers.get("x-death"));
    }
}