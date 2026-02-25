package com.jompastech.emailSender.consumer;

import com.jompastech.emailSender.configuration.RabbitMq;
import com.jompastech.emailSender.mapper.EmailMapper;
import com.jompastech.emailSender.model.entity.Email;
import com.jompastech.emailSender.model.event.EmailEventDTO;
import com.jompastech.emailSender.repository.EmailRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class EmailConsumer {

    private final EmailRepository emailRepository;

    public EmailConsumer(EmailRepository emailRepository){
        this.emailRepository = emailRepository;
    }

    @RabbitListener(queues = RabbitMq.QUEUE)
    public void receive(EmailEventDTO event) {
        Email email = EmailMapper.toEntity(event);

        emailRepository.save(email);
        System.out.println("Email enviado e salvo com sucesso");
     }
}
