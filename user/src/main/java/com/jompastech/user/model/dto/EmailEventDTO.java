package com.jompastech.user.model.dto;

import java.io.Serializable;
import java.util.UUID;

public record EmailEventDTO(
        UUID userId,
        String emailFrom,
        String emailTo,
        String subject,
        String body,
        String eventType
) implements Serializable {

}
