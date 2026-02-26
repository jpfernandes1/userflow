package com.jompastech.emailSender.model.event;

import java.io.Serializable;
import java.util.UUID;

public record EmailEventDTO(
        UUID eventId,
        UUID userId,
        String emailFrom,
        String emailTo,
        String subject,
        String body
) implements Serializable {}