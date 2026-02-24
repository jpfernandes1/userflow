package com.jompastech.emailSender.model.dto;

import java.util.UUID;

public record EmailDTO(
        UUID id,
        String subject,
        String body
) {}
