package com.jompastech.emailSender.consumer;

import com.jompastech.emailSender.configuration.RabbitMq;
import com.jompastech.emailSender.model.event.EmailEventDTO;
import com.jompastech.emailSender.service.EmailService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class EmailDeadLetterConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(EmailDeadLetterConsumer.class);

    private final EmailService emailService;

    public EmailDeadLetterConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = RabbitMq.DLQ)
    public void handleDeadLetter(EmailEventDTO event, Message message) {

        try {
            MDC.put("eventId", event.eventId().toString());
            log.error("Message moved to DLQ");
            var headers = message.getMessageProperties().getHeaders();
            log.error("x-death header: {}", headers.get("x-death"));
            emailService.incrementFinalFailure();

        } finally {
            MDC.clear();
        }
    }
}