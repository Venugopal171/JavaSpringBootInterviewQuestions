package com.JavaInterviewQuestions.JavaInterviewQuestions.controller;

import com.JavaInterviewQuestions.JavaInterviewQuestions.dto.PaymentVerificationRequest;
import com.JavaInterviewQuestions.JavaInterviewQuestions.dto.RegistrationRequest;
import com.JavaInterviewQuestions.JavaInterviewQuestions.entity.Order;
import com.JavaInterviewQuestions.JavaInterviewQuestions.entity.User;
import com.JavaInterviewQuestions.JavaInterviewQuestions.repository.UserRepository;
import com.JavaInterviewQuestions.JavaInterviewQuestions.service.PaymentService;
import com.razorpay.RazorpayException;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/payment")
@Slf4j
public class PaymentController {
    
    private final PaymentService paymentService;
    private final UserRepository userRepository;
    
    public PaymentController(PaymentService paymentService, UserRepository userRepository) {
        this.paymentService = paymentService;
        this.userRepository = userRepository;
    }
    
    // Redirect if someone accesses create-order via GET (browser URL)
    @GetMapping("/create-order")
    public String redirectToRegister() {
        return "redirect:/register";
    }
    
    @PostMapping("/create-order")
    public String createOrder(@Valid @ModelAttribute RegistrationRequest request,
                              BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("price", paymentService.getProductPrice());
            return "register";
        }
        
        try {
            // Create or get user
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseGet(() -> {
                        User newUser = User.builder()
                                .name(request.getName())
                                .email(request.getEmail())
                                .phoneNumber(request.getPhoneNumber())
                                .build();
                        return userRepository.save(newUser);
                    });
            
            // Create Razorpay order
            Order order = paymentService.createOrder(user);
            
            model.addAttribute("orderId", order.getRazorpayOrderId());
            model.addAttribute("amount", order.getAmount());
            model.addAttribute("razorpayKey", paymentService.getRazorpayKeyId());
            model.addAttribute("user", user);
            
            return "payment";
            
        } catch (RazorpayException e) {
            log.error("Error creating order", e);
            model.addAttribute("error", "Payment initialization failed. Please try again.");
            model.addAttribute("price", paymentService.getProductPrice());
            return "register";
        }
    }
    
    @PostMapping("/verify")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> verifyPayment(@RequestBody PaymentVerificationRequest request) {
        Map<String, Object> response = new HashMap<>();
        
        boolean isValid = paymentService.verifyPayment(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );
        
        if (isValid) {
            boolean emailSent = paymentService.processSuccessfulPayment(
                    request.getRazorpayOrderId(),
                    request.getRazorpayPaymentId(),
                    request.getRazorpaySignature()
            );
            response.put("success", true);
            response.put("emailSent", emailSent);
            response.put("orderId", request.getRazorpayOrderId());
            response.put("message", emailSent 
                    ? "Payment successful! Check your email for the PDF." 
                    : "Payment successful! Email delivery failed - use the resend button.");
        } else {
            response.put("success", false);
            response.put("message", "Payment verification failed.");
        }
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/resend-email")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> resendEmail(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String orderId = request.get("orderId");
            if (orderId == null || orderId.isEmpty()) {
                response.put("success", false);
                response.put("message", "Order ID is required.");
                return ResponseEntity.badRequest().body(response);
            }
            
            boolean emailSent = paymentService.resendEmail(orderId);
            response.put("success", emailSent);
            response.put("message", emailSent 
                    ? "Email sent successfully! Please check your inbox." 
                    : "Failed to send email. Please try again or contact support.");
            
        } catch (RuntimeException e) {
            log.error("Error resending email", e);
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}

