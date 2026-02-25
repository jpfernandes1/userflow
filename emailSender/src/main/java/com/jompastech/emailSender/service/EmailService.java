package com.jompastech.emailSender.service;

import com.jompastech.emailSender.mapper.EmailMapper;
import com.jompastech.emailSender.model.dto.EmailResponseDTO;
import com.jompastech.emailSender.model.entity.Email;
import com.jompastech.emailSender.repository.EmailRepository;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EmailService {

    @Autowired
    EmailRepository emailRepository;

    public void sendEmail(EmailResponseDTO email){
        Email Email = null;
        emailRepository.save(Email);
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

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
