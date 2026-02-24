package com.jompastech.emailSender.repository;

import com.jompastech.emailSender.model.entity.Email;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmailRepository extends JpaRepository<Email, UUID> {
}
