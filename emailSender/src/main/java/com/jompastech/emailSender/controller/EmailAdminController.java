package com.jompastech.emailSender.controller;

import com.jompastech.emailSender.configuration.RabbitMq;
import com.jompastech.emailSender.model.event.EmailEventDTO;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/emails")
public class EmailAdminController {

    private final RabbitTemplate rabbitTemplate;

    public EmailAdminController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/reprocess")
    public String reprocessMessage(@RequestBody EmailEventDTO event) {

        rabbitTemplate.convertAndSend(
                RabbitMq.EXCHANGE,
                RabbitMq.ROUTING_KEY,
                event
        );

        return "Message reprocessed";
    }
}