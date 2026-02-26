package com.jompastech.user.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "outbox")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String aggregateType; // USER
    private UUID aggregateId;

    private String eventType; // USER_CREATED

    @Column(columnDefinition = "TEXT")
    private String payload;

    private boolean processed = false;

    private LocalDateTime createdAt = LocalDateTime.now();

}