package com.ems.employee_management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PostConstruct;

@Service
public class EmailService {

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @PostConstruct
    public void checkPassword() {
        System.out.println("Password loaded: "
                + !mailPassword.isBlank());
        System.out.println("Password length: "
                + mailPassword.length());
    }

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String senderAddress;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    @PostConstruct
    void logMailConfigurationStatus() {
        if (smtpPassword == null || smtpPassword.isBlank()) {
            log.warn("SMTP password is missing in this backend runtime; set SPRING_MAIL_PASSWORD in the active run configuration and restart");
        } else {
            log.info("SMTP password is configured for this backend runtime");
        }
    }

    @Async
    public void sendEmail(String to, String subject, String body) {
        if (to == null || to.isBlank()) {
            log.warn("Email not sent because the recipient address is blank");
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        if (senderAddress != null && !senderAddress.isBlank()) message.setFrom(senderAddress);
        message.setSubject(subject);
        message.setText(body);
        try {
            mailSender.send(message);
            log.info("Email sent to {} with subject '{}'", to, subject);
        } catch (MailException exception) {
            log.error("Email delivery failed for {} ({}): {}", to, exception.getClass().getSimpleName(), exception.getMessage());
        }
    }
}
