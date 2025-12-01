package com.JavaInterviewQuestions.JavaInterviewQuestions.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Email
    @NotBlank
    @Column(unique = true)
    private String email;
    
    @NotBlank
    @Column(length = 15)
    private String phoneNumber;
    
    private String name;
    
    private LocalDateTime createdAt;
    
    private boolean pdfDelivered;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

