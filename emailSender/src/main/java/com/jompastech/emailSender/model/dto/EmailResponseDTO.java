package com.jompastech.emailSender.model.dto;

import java.util.UUID;

public record EmailResponseDTO(
        UUID id,
        String subject,
        String body
) {}
