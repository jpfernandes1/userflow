package com.jompastech.user.service;

import com.jompastech.user.model.dto.EmailEventDTO;
import com.jompastech.user.model.dto.UserDTO;
import com.jompastech.user.model.entity.User;
import com.jompastech.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final EmailPublisher emailPublisher;

    public UserService(UserRepository repository, EmailPublisher emailPublisher) {
        this.userRepository = repository;
        this.emailPublisher = emailPublisher;
    }

    public List<User> getAll(){
        return userRepository.findAll();
    }

    // getById
    public User getById(UUID id){
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Email not found"));
    }

    // Create
    public User createUser(UserDTO userDTO){
        User user = new User();
        user.setName(userDTO.getName());
        user.setEmail(userDTO.getEmail());

        User savedUser = userRepository.save(user);

        // Send the event to email-service queue
        sendEmailEvent(
                savedUser,
                "Welcome!",
                "Your account has been created successfully!",
                "USER_CREATED"
        );
        return savedUser;
    }

    // Delete
    public void deleteUser(UUID id){
        User user = userRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("User not found"));

        userRepository.deleteById(id);
        sendEmailEvent(
                user,
                "Account removed",
                "Your account has been removed successfully",
                "USER_DELETED"
        );
    }

    // Update
    public User updateUser(UUID id, UserDTO dto){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(dto.getName());
        user.setEmail(dto.getEmail());

        User updatedUser = userRepository.save(user);

        sendEmailEvent(
                updatedUser,
                "Data updated",
                "Your data has been updated succesfully!",
                "USER_UPDATED"
        );
        return updatedUser;
    }


    private void sendEmailEvent(User user, String subject, String body, String eventType) {
        emailPublisher.publish(
                new EmailEventDTO(
                        user.getId(),
                        "no-reply@system.com",
                        user.getEmail(),
                        subject,
                        body,
                        eventType
                )
        );
    }

}
