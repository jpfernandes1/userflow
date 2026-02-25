package com.jompastech.emailSender.controller;

import com.jompastech.emailSender.model.dto.EmailResponseDTO;
import com.jompastech.emailSender.service.EmailService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/emails")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping
    public List<EmailResponseDTO> getAllEmails() {
        return emailService.findAll();
    }

    @GetMapping("/{id}")
    public EmailResponseDTO getEmail(@PathVariable UUID id) {
        return emailService.findById(id);
    }

    @DeleteMapping("/{id}")
    public void deleteEmail(@PathVariable UUID id) {
        emailService.deleteById(id);
    }
}