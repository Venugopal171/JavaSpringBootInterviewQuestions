package com.JavaInterviewQuestions.JavaInterviewQuestions.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    
    private String razorpayOrderId;
    
    private String razorpayPaymentId;
    
    private String razorpaySignature;
    
    private Integer amount; // in paise
    
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime paidAt;
    
    @Builder.Default
    private boolean emailSent = false;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = OrderStatus.CREATED;
    }
    
    public enum OrderStatus {
        CREATED, PAID, FAILED, REFUNDED
    }
}

