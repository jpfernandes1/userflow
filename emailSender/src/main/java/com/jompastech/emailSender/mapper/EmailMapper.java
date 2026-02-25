package com.jompastech.emailSender.mapper;

import com.jompastech.emailSender.model.event.EmailEventDTO;
import com.jompastech.emailSender.model.dto.EmailResponseDTO;
import com.jompastech.emailSender.model.entity.Email;
import com.jompastech.emailSender.model.enums.EmailStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class EmailMapper {

    private EmailMapper(){}

    public static EmailResponseDTO toDTO(Email email){
        return new EmailResponseDTO(
                email.getEmailId(),
                email.getEmailSubject(),
                email.getEmailBody()
        );
    }
    public static List<EmailResponseDTO> toDTOList(List<Email> emails){
        return emails.stream()
                .map(EmailMapper::toDTO)
                .collect(Collectors.toList());
    }

    public static Email toEntity(EmailEventDTO dto) {

        Email email = new Email();

        email.setUserId(dto.userId());
        email.setEmailFrom(dto.emailFrom());
        email.setEmailTo(dto.emailTo());
        email.setEmailSubject(dto.subject());
        email.setEmailBody(dto.body());
        email.setSendDateEmail(LocalDateTime.now());
        email.setEmailStatus(EmailStatus.PENDING);

        return email;
    }
}
