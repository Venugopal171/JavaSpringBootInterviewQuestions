package com.JavaInterviewQuestions.JavaInterviewQuestions.service;

import com.JavaInterviewQuestions.JavaInterviewQuestions.entity.User;
import com.JavaInterviewQuestions.JavaInterviewQuestions.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {
    
    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    
    @Value("${product.pdf.path}")
    private Resource pdfResource;
    
    @Value("${product.name}")
    private String productName;
    
    public EmailService(JavaMailSender mailSender, UserRepository userRepository) {
        this.mailSender = mailSender;
        this.userRepository = userRepository;
    }
    
    /**
     * Send PDF email to user
     * @param user the user to send email to
     * @return true if email was sent successfully, false otherwise
     */
    public boolean sendPdfEmail(User user) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(user.getEmail());
            helper.setSubject("🎉 Your " + productName + " is here!");
            
            // Check if PDF exists before attaching
            boolean pdfAttached = false;
            if (pdfResource != null && pdfResource.exists()) {
                helper.addAttachment("Java-Spring-Interview-Questions.pdf", pdfResource);
                helper.setText(buildEmailContent(user.getName()), true);
                pdfAttached = true;
            } else {
                // Send email without attachment but with a note
                helper.setText(buildEmailContentWithoutPdf(user.getName()), true);
                log.warn("PDF file not found, sending email without attachment to: {}", user.getEmail());
            }
            
            mailSender.send(message);
            
            user.setPdfDelivered(pdfAttached);
            userRepository.save(user);
            log.info("Email sent successfully to: {} (PDF attached: {})", user.getEmail(), pdfAttached);
            return true;
            
        } catch (MessagingException e) {
            log.error("Failed to send email to: {} - MessagingException: {}", user.getEmail(), e.getMessage());
            return false;
        } catch (Exception e) {
            // Catch all other exceptions (including MailAuthenticationException)
            // so payment verification doesn't fail
            log.error("Failed to send email to: {} - Error: {}", user.getEmail(), e.getMessage());
            return false;
        }
    }
    
    private String buildEmailContent(String name) {
        return """
            <html>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; background: #0f0f23; color: #cccccc; padding: 40px;">
                <div style="max-width: 600px; margin: 0 auto; background: #1a1a2e; border-radius: 16px; padding: 40px; border: 1px solid #16213e;">
                    <h1 style="color: #00d9ff; margin-bottom: 20px;">🚀 Thank You, %s!</h1>
                    <p style="font-size: 16px; line-height: 1.8;">
                        Your purchase is complete! Attached is your <strong style="color: #ffd700;">Java & Spring Knowledge Boost</strong> eBook.
                    </p>
                    <div style="background: #16213e; border-left: 4px solid #00d9ff; padding: 20px; margin: 30px 0; border-radius: 8px;">
                        <p style="margin: 0; color: #e0e0e0;">
                            📚 <strong>961 Interview Questions</strong><br>
                            ✅ Java Core + Streams + Scenarios<br>
                            ✅ Spring Boot + Microservices<br>
                            ✅ REST API + Testing<br>
                            ✅ Coding Problems + Cheat Sheets
                        </p>
                    </div>
                    <p style="color: #888; font-size: 14px;">
                        Keep this email safe - it's your proof of purchase!
                    </p>
                    <p style="margin-top: 30px; color: #00d9ff;">
                        Best of luck with your interviews! 💪
                    </p>
                </div>
            </body>
            </html>
            """.formatted(name);
    }
    
    private String buildEmailContentWithoutPdf(String name) {
        return """
            <html>
            <body style="font-family: 'Segoe UI', Arial, sans-serif; background: #0f0f23; color: #cccccc; padding: 40px;">
                <div style="max-width: 600px; margin: 0 auto; background: #1a1a2e; border-radius: 16px; padding: 40px; border: 1px solid #16213e;">
                    <h1 style="color: #00d9ff; margin-bottom: 20px;">🚀 Thank You, %s!</h1>
                    <p style="font-size: 16px; line-height: 1.8;">
                        Your purchase is complete! Your <strong style="color: #ffd700;">Java & Spring Knowledge Boost</strong> eBook will be sent to you shortly.
                    </p>
                    <div style="background: #16213e; border-left: 4px solid #ffc107; padding: 20px; margin: 30px 0; border-radius: 8px;">
                        <p style="margin: 0; color: #ffc107;">
                            ⏳ <strong>Your PDF is being prepared!</strong><br>
                            <span style="color: #e0e0e0;">We'll send it to this email address within 24 hours.</span>
                        </p>
                    </div>
                    <div style="background: #16213e; border-left: 4px solid #00d9ff; padding: 20px; margin: 30px 0; border-radius: 8px;">
                        <p style="margin: 0; color: #e0e0e0;">
                            📚 <strong>961 Interview Questions</strong><br>
                            ✅ Java Core + Streams + Scenarios<br>
                            ✅ Spring Boot + Microservices<br>
                            ✅ REST API + Testing<br>
                            ✅ Coding Problems + Cheat Sheets
                        </p>
                    </div>
                    <p style="color: #888; font-size: 14px;">
                        Keep this email safe - it's your proof of purchase!
                    </p>
                    <p style="margin-top: 30px; color: #00d9ff;">
                        Best of luck with your interviews! 💪
                    </p>
                </div>
            </body>
            </html>
            """.formatted(name);
    }
}

