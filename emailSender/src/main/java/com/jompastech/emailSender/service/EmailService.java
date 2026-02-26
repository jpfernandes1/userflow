package com.jompastech.emailSender.service;

import com.jompastech.emailSender.mapper.EmailMapper;
import com.jompastech.emailSender.model.dto.EmailResponseDTO;
import com.jompastech.emailSender.model.entity.Email;
import com.jompastech.emailSender.model.enums.EmailStatus;
import com.jompastech.emailSender.model.event.EmailEventDTO;
import com.jompastech.emailSender.repository.EmailRepository;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    EmailRepository emailRepository;

    public void processEmailEvent(EmailEventDTO event) {

        log.info("Received email event {}", event.eventId());

        Email email = EmailMapper.toEntity(event);
        email.setEmailStatus(EmailStatus.PENDING);

        try {
            email = emailRepository.save(email);

        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate event detected: {}", event.eventId());
            return;
        }

        try {
            email.setEmailStatus(EmailStatus.PROCESSING);
            emailRepository.save(email);
            sendEmail(email);
            email.setEmailStatus(EmailStatus.SENT);

        } catch (Exception e) {
            email.setEmailStatus(EmailStatus.FAILED);
            log.error("Failed to send email to {}", email.getEmailTo(), e);
            throw e; // keeps rabbit retry
        }
        emailRepository.save(email);
    }

    public List<EmailResponseDTO> findAll(){
        List<Email> emailsList = emailRepository.findAll();
        return EmailMapper.toDTOList(emailsList);
    }

    public EmailResponseDTO findById(UUID id) {
        return emailRepository.findById(id)
                .map(EmailMapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Email not found"));
    }

    public void deleteById(UUID id){
        emailRepository.deleteById(id);
    }

    private void sendEmail(Email email) {

        // Simulation
        if(email.getEmailTo().contains("fail")) {
            throw new RuntimeException("Simulated failure");
        }

        log.info("Email sent to {}", email.getEmailTo());
    }

    public EmailResponseDTO retryEmail(UUID id) {

        Email email = emailRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        if (email.getEmailStatus() != EmailStatus.FAILED) {
            throw new IllegalStateException("Only FAILED emails can be retried");
        }

        try {
            email.setEmailStatus(EmailStatus.PROCESSING);
            emailRepository.save(email);

            sendEmail(email);

            email.setEmailStatus(EmailStatus.SENT);
            emailRepository.save(email);

            log.info("Email retried successfully: {}", email.getEmailId());

        } catch (Exception e) {

            email.setEmailStatus(EmailStatus.FAILED);
            emailRepository.save(email);

            log.error("Retry failed for email {}", email.getEmailId(), e);

            throw e;
        }

        return EmailMapper.toDTO(email);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
