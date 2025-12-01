package com.JavaInterviewQuestions.JavaInterviewQuestions.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegistrationRequest {
    
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email")
    private String email;
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter valid 10-digit Indian mobile number")
    private String phoneNumber;
}

