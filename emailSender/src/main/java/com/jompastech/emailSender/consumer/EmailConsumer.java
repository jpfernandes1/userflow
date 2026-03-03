package com.jompastech.emailSender.consumer;

import com.jompastech.emailSender.configuration.RabbitMq;
import com.jompastech.emailSender.model.event.EmailEventDTO;
import com.jompastech.emailSender.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EmailConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(EmailConsumer.class);

    private final EmailService emailService;

    public EmailConsumer(EmailService emailService){
        this.emailService = emailService;
    }

    @RabbitListener(queues = RabbitMq.QUEUE)
    public void receive(EmailEventDTO event) {

        try {
            MDC.put("eventId", event.eventId().toString());
            log.info("Email event received from queue");
            emailService.processEmailEvent(event);
            log.info("Email event processed successfully");

        } catch (Exception e) {

            log.error("Error processing email event", e);
            throw e; // import for retry

        } finally {
            MDC.clear();
        }
    }
}