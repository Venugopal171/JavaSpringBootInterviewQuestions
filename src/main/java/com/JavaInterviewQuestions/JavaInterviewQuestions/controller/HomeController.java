package com.JavaInterviewQuestions.JavaInterviewQuestions.controller;

import com.JavaInterviewQuestions.JavaInterviewQuestions.dto.RegistrationRequest;
import com.JavaInterviewQuestions.JavaInterviewQuestions.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    
    private final PaymentService paymentService;
    
    public HomeController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }
    
    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("price", paymentService.getProductPrice());
        return "index";
    }
    
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationRequest", new RegistrationRequest());
        model.addAttribute("price", paymentService.getProductPrice());
        return "register";
    }
    
    @GetMapping("/success")
    public String paymentSuccess() {
        return "success";
    }
}

