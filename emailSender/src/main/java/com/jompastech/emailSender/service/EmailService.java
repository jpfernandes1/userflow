package com.jompastech.emailSender.service;

import com.jompastech.emailSender.mapper.EmailMapper;
import com.jompastech.emailSender.model.dto.EmailResponseDTO;
import com.jompastech.emailSender.model.entity.Email;
import com.jompastech.emailSender.model.enums.EmailStatus;
import com.jompastech.emailSender.model.event.EmailEventDTO;
import com.jompastech.emailSender.repository.EmailRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final EmailRepository emailRepository;
    private final Counter emailSuccessCounter;
    private final Counter emailAttemptFailureCounter;
    private final Counter emailFinalFailureCounter;
    private final Timer emailSuccessTimer;
    private final Timer emailFailureTimer;
    private final Timer emailSendSuccessTimer;
    private final Timer emailSendFailureTimer;

    public void incrementFinalFailure() {
        emailFinalFailureCounter.increment();
    }

    public EmailService(EmailRepository emailRepository,
                        MeterRegistry registry) {
        this.emailRepository = emailRepository;
        this.emailSuccessCounter =
                Counter.builder("email.process.success")
                        .description("Emails processed successfully")
                        .register(registry);

        this.emailAttemptFailureCounter =
                Counter.builder("email.process.attempt.failure")
                        .description("Email processing attempt failures")
                        .register(registry);

        this.emailFinalFailureCounter =
                Counter.builder("email.process.final.failure")
                        .description("Emails that ended in DLQ")
                        .register(registry);
        this.emailSuccessTimer =
                Timer.builder("email.process.duration")
                        .description("Email processing duration")
                        .tag("status", "success")
                        .register(registry);

        this.emailFailureTimer =
                Timer.builder("email.process.duration")
                        .description("Email processing duration")
                        .tag("status", "failure")
                        .register(registry);
        this.emailSendSuccessTimer =
                Timer.builder("email.send.duration")
                        .description("Email sending duration")
                        .tag("status", "success")
                        .register(registry);

        this.emailSendFailureTimer =
                Timer.builder("email.send.duration")
                        .description("Email sending duration")
                        .tag("status", "failure")
                        .register(registry);
    }

    public void processEmailEvent(EmailEventDTO event) {

        Timer.Sample sample = Timer.start();

        try {

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

                Timer.Sample sendSample = Timer.start();
                try {
                    sendEmail(email);
                    sendSample.stop(emailSendSuccessTimer);
                } catch (Exception ex){
                    sendSample.stop((emailSendFailureTimer));
                    throw ex;
                }
                email.setEmailStatus(EmailStatus.SENT);
                emailSuccessCounter.increment();
                emailRepository.save(email);
                sample.stop(emailSuccessTimer);

            } catch (Exception e) {

                email.setEmailStatus(EmailStatus.FAILED);
                log.error("Failed to send email to {}", email.getEmailTo(), e);
                emailAttemptFailureCounter.increment();
                emailRepository.save(email);
                sample.stop(emailFailureTimer);
                throw e; // keeps rabbit retry
            }
        } catch (Exception e) {
            throw e;
        }
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
            emailAttemptFailureCounter.increment();
            log.error("Retry failed for email {}", email.getEmailId(), e);
            throw e;
        }
        return EmailMapper.toDTO(email);
    }
}
