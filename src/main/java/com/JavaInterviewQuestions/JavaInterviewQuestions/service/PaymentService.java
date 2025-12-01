package com.JavaInterviewQuestions.JavaInterviewQuestions.service;

import com.JavaInterviewQuestions.JavaInterviewQuestions.entity.Order;
import com.JavaInterviewQuestions.JavaInterviewQuestions.entity.User;
import com.JavaInterviewQuestions.JavaInterviewQuestions.repository.OrderRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.Formatter;

@Service
@Slf4j
public class PaymentService {
    
    @Value("${razorpay.key.id}")
    private String razorpayKeyId;
    
    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;
    
    @Value("${product.price}")
    private Integer productPrice;
    
    private final OrderRepository orderRepository;
    private final EmailService emailService;
    
    public PaymentService(OrderRepository orderRepository, EmailService emailService) {
        this.orderRepository = orderRepository;
        this.emailService = emailService;
    }
    
    public Order createOrder(User user) throws RazorpayException {
        RazorpayClient client = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
        
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", productPrice * 100); // amount in paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "order_" + System.currentTimeMillis());
        
        com.razorpay.Order razorpayOrder = client.orders.create(orderRequest);
        
        Order order = Order.builder()
                .user(user)
                .razorpayOrderId(razorpayOrder.get("id"))
                .amount(productPrice * 100)
                .status(Order.OrderStatus.CREATED)
                .build();
        
        return orderRepository.save(order);
    }
    
    public boolean verifyPayment(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            String generatedSignature = hmacSha256(payload, razorpayKeySecret);
            return generatedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Payment verification failed", e);
            return false;
        }
    }
    
    /**
     * Process successful payment and send email
     * @return true if email was sent successfully, false otherwise
     */
    public boolean processSuccessfulPayment(String orderId, String paymentId, String signature) {
        Order order = orderRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        order.setRazorpayPaymentId(paymentId);
        order.setRazorpaySignature(signature);
        order.setStatus(Order.OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        
        // Send PDF via email and track status
        boolean emailSent = emailService.sendPdfEmail(order.getUser());
        order.setEmailSent(emailSent);
        orderRepository.save(order);
        
        return emailSent;
    }
    
    /**
     * Resend PDF email for an existing paid order
     * @param orderId the Razorpay order ID
     * @return true if email was sent successfully
     */
    public boolean resendEmail(String orderId) {
        Order order = orderRepository.findByRazorpayOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        
        if (order.getStatus() != Order.OrderStatus.PAID) {
            throw new RuntimeException("Order is not paid");
        }
        
        boolean emailSent = emailService.sendPdfEmail(order.getUser());
        order.setEmailSent(emailSent);
        orderRepository.save(order);
        
        return emailSent;
    }
    
    private String hmacSha256(String data, String secret) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hash = mac.doFinal(data.getBytes());
        return toHexString(hash);
    }
    
    private String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }
    
    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }
    
    public Integer getProductPrice() {
        return productPrice;
    }
}

