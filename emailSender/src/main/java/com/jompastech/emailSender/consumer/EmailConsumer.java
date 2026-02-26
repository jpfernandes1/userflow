package com.jompastech.emailSender.consumer;

import com.jompastech.emailSender.configuration.RabbitMq;
import com.jompastech.emailSender.model.event.EmailEventDTO;
import com.jompastech.emailSender.service.EmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EmailConsumer {

    private final EmailService emailService;

    public EmailConsumer(EmailService emailService){
        this.emailService = emailService;
    }

    @RabbitListener(queues = RabbitMq.QUEUE)
    public void receive(EmailEventDTO event) {
        emailService.processEmailEvent(event);
     }
}
