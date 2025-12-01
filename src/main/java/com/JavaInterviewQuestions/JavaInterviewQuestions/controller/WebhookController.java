package com.JavaInterviewQuestions.JavaInterviewQuestions.controller;

import com.JavaInterviewQuestions.JavaInterviewQuestions.entity.Order;
import com.JavaInterviewQuestions.JavaInterviewQuestions.repository.OrderRepository;
import com.JavaInterviewQuestions.JavaInterviewQuestions.service.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.Formatter;
import java.util.Optional;

@RestController
@RequestMapping("/webhook")
@Slf4j
public class WebhookController {

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    private final OrderRepository orderRepository;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    public WebhookController(OrderRepository orderRepository, EmailService emailService, ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        log.info("Received Razorpay webhook");

        // Verify webhook signature
        if (signature != null && !verifyWebhookSignature(payload, signature)) {
            log.warn("Invalid webhook signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        try {
            JsonNode rootNode = objectMapper.readTree(payload);
            String event = rootNode.path("event").asText();

            log.info("Processing webhook event: {}", event);

            switch (event) {
                case "payment.captured":
                    handlePaymentCaptured(rootNode);
                    break;
                case "payment.failed":
                    handlePaymentFailed(rootNode);
                    break;
                case "order.paid":
                    handleOrderPaid(rootNode);
                    break;
                default:
                    log.info("Unhandled webhook event: {}", event);
            }

            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {
            log.error("Error processing webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }
    }

    private void handlePaymentCaptured(JsonNode rootNode) {
        try {
            JsonNode paymentEntity = rootNode.path("payload").path("payment").path("entity");
            String orderId = paymentEntity.path("order_id").asText();
            String paymentId = paymentEntity.path("id").asText();
            String status = paymentEntity.path("status").asText();

            log.info("Payment captured - Order ID: {}, Payment ID: {}, Status: {}", orderId, paymentId, status);

            Optional<Order> orderOpt = orderRepository.findByRazorpayOrderId(orderId);
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                
                // Only process if not already paid
                if (order.getStatus() != Order.OrderStatus.PAID) {
                    order.setRazorpayPaymentId(paymentId);
                    order.setStatus(Order.OrderStatus.PAID);
                    order.setPaidAt(LocalDateTime.now());
                    orderRepository.save(order);

                    // Send PDF email
                    emailService.sendPdfEmail(order.getUser());
                    log.info("Order {} marked as PAID via webhook", orderId);
                } else {
                    log.info("Order {} already marked as PAID", orderId);
                }
            } else {
                log.warn("Order not found for orderId: {}", orderId);
            }
        } catch (Exception e) {
            log.error("Error handling payment.captured event", e);
        }
    }

    private void handlePaymentFailed(JsonNode rootNode) {
        try {
            JsonNode paymentEntity = rootNode.path("payload").path("payment").path("entity");
            String orderId = paymentEntity.path("order_id").asText();
            String paymentId = paymentEntity.path("id").asText();
            String errorCode = paymentEntity.path("error_code").asText();
            String errorDescription = paymentEntity.path("error_description").asText();

            log.warn("Payment failed - Order ID: {}, Payment ID: {}, Error: {} - {}", 
                    orderId, paymentId, errorCode, errorDescription);

            Optional<Order> orderOpt = orderRepository.findByRazorpayOrderId(orderId);
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                order.setRazorpayPaymentId(paymentId);
                order.setStatus(Order.OrderStatus.FAILED);
                orderRepository.save(order);
                log.info("Order {} marked as FAILED via webhook", orderId);
            }
        } catch (Exception e) {
            log.error("Error handling payment.failed event", e);
        }
    }

    private void handleOrderPaid(JsonNode rootNode) {
        try {
            JsonNode orderEntity = rootNode.path("payload").path("order").path("entity");
            String orderId = orderEntity.path("id").asText();
            String status = orderEntity.path("status").asText();

            log.info("Order paid event - Order ID: {}, Status: {}", orderId, status);

            Optional<Order> orderOpt = orderRepository.findByRazorpayOrderId(orderId);
            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                if (order.getStatus() != Order.OrderStatus.PAID) {
                    order.setStatus(Order.OrderStatus.PAID);
                    order.setPaidAt(LocalDateTime.now());
                    orderRepository.save(order);

                    // Send PDF email
                    emailService.sendPdfEmail(order.getUser());
                    log.info("Order {} marked as PAID via order.paid webhook", orderId);
                }
            }
        } catch (Exception e) {
            log.error("Error handling order.paid event", e);
        }
    }

    private boolean verifyWebhookSignature(String payload, String signature) {
        try {
            String generatedSignature = hmacSha256(payload, webhookSecret);
            return generatedSignature.equals(signature);
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
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
}

