package com.JavaInterviewQuestions.JavaInterviewQuestions.service;

import com.JavaInterviewQuestions.JavaInterviewQuestions.entity.User;
import com.JavaInterviewQuestions.JavaInterviewQuestions.repository.UserRepository;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

@Service
@Slf4j
public class EmailService {
    
    private final UserRepository userRepository;
    
    @Value("${sendgrid.api.key}")
    private String sendGridApiKey;
    
    @Value("${sendgrid.from.email}")
    private String fromEmail;
    
    @Value("${sendgrid.from.name}")
    private String fromName;
    
    @Value("${product.pdf.path}")
    private Resource pdfResource;
    
    @Value("${product.name}")
    private String productName;
    
    public EmailService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    /**
     * Send PDF email to user using SendGrid
     * @param user the user to send email to
     * @return true if email was sent successfully, false otherwise
     */
    public boolean sendPdfEmail(User user) {
        try {
            Email from = new Email(fromEmail, fromName);
            Email to = new Email(user.getEmail());
            String subject = "🎉 Your " + productName + " is here!";
            
            Content content = new Content("text/html", buildEmailContent(user.getName()));
            Mail mail = new Mail(from, subject, to, content);
            
            // Attach PDF if available
            boolean pdfAttached = false;
            if (pdfResource != null && pdfResource.exists()) {
                try (InputStream inputStream = pdfResource.getInputStream()) {
                    byte[] pdfBytes = inputStream.readAllBytes();
                    String encodedPdf = Base64.getEncoder().encodeToString(pdfBytes);
                    
                    Attachments attachment = new Attachments();
                    attachment.setContent(encodedPdf);
                    attachment.setType("application/pdf");
                    attachment.setFilename("Java-Spring-Interview-Questions.pdf");
                    attachment.setDisposition("attachment");
                    
                    mail.addAttachments(attachment);
                    pdfAttached = true;
                    log.info("PDF attached successfully for: {}", user.getEmail());
                } catch (Exception e) {
                    log.warn("Could not attach PDF: {}. Sending email without attachment.", e.getMessage());
                    mail = new Mail(from, subject, to, new Content("text/html", buildEmailContentWithoutPdf(user.getName())));
                }
            } else {
                log.warn("PDF file not found, sending email without attachment to: {}", user.getEmail());
                mail = new Mail(from, subject, to, new Content("text/html", buildEmailContentWithoutPdf(user.getName())));
            }
            
            // Send via SendGrid
            SendGrid sg = new SendGrid(sendGridApiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sg.api(request);
            
            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
                user.setPdfDelivered(pdfAttached);
                userRepository.save(user);
                log.info("Email sent successfully via SendGrid to: {} (Status: {}, PDF attached: {})", 
                        user.getEmail(), response.getStatusCode(), pdfAttached);
                return true;
            } else {
                log.error("SendGrid API error - Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                return false;
            }
            
        } catch (IOException e) {
            log.error("Failed to send email to: {} - IOException: {}", user.getEmail(), e.getMessage());
            return false;
        } catch (Exception e) {
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
                    <div style="background: #0f0f23; padding: 15px; border-radius: 8px; margin: 20px 0;">
                        <p style="margin: 0; color: #ffc107; font-size: 13px;">
                            💡 <strong>Tip:</strong> To ensure you receive future emails, add <strong>backendwithvenu@gmail.com</strong> to your contacts!
                        </p>
                    </div>
                    <p style="margin-top: 30px; color: #00d9ff;">
                        Best of luck with your interviews! 💪<br>
                        <span style="color: #888;">- backendwithvenu</span>
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
                        Best of luck with your interviews! 💪<br>
                        <span style="color: #888;">- backendwithvenu</span>
                    </p>
                </div>
            </body>
            </html>
            """.formatted(name);
    }
}
