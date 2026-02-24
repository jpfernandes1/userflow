package com.jompastech.emailSender.model.entity;

import com.jompastech.emailSender.model.enums.EmailStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="TB_EMAIL")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Email {

    private String emailId;
    private String userId;
    private String emailFrom;
    private String emailTo;
    private String emailSubject;
    @Column(columnDefinition = "BODY")
    private String emailBody;
    private EmailStatus emailstatus;
    private LocalDateTime sendDateEmail;

}
