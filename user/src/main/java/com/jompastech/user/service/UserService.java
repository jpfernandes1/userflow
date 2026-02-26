package com.jompastech.user.service;

import com.jompastech.user.model.dto.EmailEventDTO;
import com.jompastech.user.model.dto.UserDTO;
import com.jompastech.user.model.entity.OutboxEvent;
import com.jompastech.user.model.entity.User;
import com.jompastech.user.repository.OutboxRepository;
import com.jompastech.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public UserService(UserRepository userRepository,
                       OutboxRepository outboxRepository,
                       ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public List<User> getAll(){
        return userRepository.findAll();
    }

    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }


    @Transactional
    public User createUser(UserDTO userDTO) {
        User user = new User();
        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());

        User savedUser = userRepository.save(user);

        createOutboxEvent(
                savedUser,
                "Welcome!",
                "Your account has been created successfully!",
                "USER_CREATED"
        );
        return savedUser;
    }

    @Transactional
    public User updateUser(UUID id, UserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());

        User updatedUser = userRepository.save(user);

        createOutboxEvent(
                updatedUser,
                "Data updated",
                "Your data has been updated successfully!",
                "USER_UPDATED"
        );
        return updatedUser;
    }

    @Transactional
    public void deleteUser(UUID id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        userRepository.deleteById(id);

        createOutboxEvent(
                user,
                "Account removed",
                "Your account has been removed successfully",
                "USER_DELETED"
        );
    }

    // OUTBOX CREATOR
    private void createOutboxEvent(User user,
                                   String subject,
                                   String body,
                                   String eventType) {

        try {
            EmailEventDTO event = new EmailEventDTO(
                    UUID.randomUUID(), // eventId
                    user.getId(),
                    "no-reply@system.com",
                    user.getEmail(),
                    subject,
                    body,
                    eventType
            );

            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outbox = new OutboxEvent();
            outbox.setAggregateType("USER");
            outbox.setAggregateId(user.getId());
            outbox.setEventType(eventType);
            outbox.setPayload(payload);
            outbox.setProcessed(false);

            outboxRepository.save(outbox);

        } catch (Exception e) {
            throw new RuntimeException("Failed to create outbox event", e);
        }
    }
}