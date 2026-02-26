package com.jompastech.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jompastech.user.config.RabbitMq;
import com.jompastech.user.model.dto.EmailEventDTO;
import com.jompastech.user.model.entity.OutboxEvent;
import com.jompastech.user.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(OutboxRepository outboxRepository,
                           RabbitTemplate rabbitTemplate,
                           ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        List<OutboxEvent> events = outboxRepository.findByProcessedFalse();

        if (events.isEmpty()) {
            return;
        }

        log.info("Found {} pending outbox event(s)", events.size());

        for (OutboxEvent event : events) {

            try {

                // Convert stored JSON to DTO
                EmailEventDTO dto = objectMapper.readValue(
                        event.getPayload(),
                        EmailEventDTO.class
                );

                // Use eventId as correlationId
                CorrelationData correlationData =
                        new CorrelationData(dto.eventId().toString());

                rabbitTemplate.convertAndSend(
                        RabbitMq.EXCHANGE,
                        RabbitMq.ROUTING_KEY,
                        dto,
                        correlationData
                );

                event.setProcessed(true);
                outboxRepository.save(event);

                log.info("Outbox event {} published successfully",
                        event.getId());

            } catch (Exception e) {

                log.error("Failed to publish outbox event {}",
                        event.getId(), e);

                // Do NOT mark as processed.
                // It will retry on next scheduler cycle.
            }
        }
    }
}