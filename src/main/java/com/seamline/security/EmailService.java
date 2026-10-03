package com.seamline.security;

import com.seamline.config.SeamlineProperties;
import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Sends the app's transactional emails — the "forgot password" one-time code and
 * account-approval notices — over plain SMTP (Gmail's free tier works with an app
 * password: no card, no third-party API key). The mail sender is built by hand
 * from {@link SeamlineProperties} rather than relying on Spring Boot's
 * auto-configured bean, so the app still starts cleanly when mail isn't
 * configured — {@link #isConfigured()} just reports false instead.
 *
 * <p>The actual send runs on a background thread ({@code @Async}) so a caller's
 * HTTP response doesn't sit waiting on Gmail's SMTP round-trip — approving a
 * registration, for instance, should feel instant. Since nothing can observe the
 * outcome once it's fired off, failures are logged here rather than thrown.
 */
@Component
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SeamlineProperties properties;

    public EmailService(SeamlineProperties properties) {
        this.properties = properties;
    }

    public boolean isConfigured() {
        SeamlineProperties.Mail mail = properties.mail();
        return mail != null && notBlank(mail.host()) && notBlank(mail.username()) && notBlank(mail.password());
    }

    /** A fresh 6-digit code, e.g. "042917". */
    public String generateCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    @Async
    public void sendOtpCode(String toEmail, String code) {
        trySend(toEmail, "Your Seamline verification code",
                "Your Seamline verification code is " + code + ". It expires in 10 minutes.\n\n"
                        + "If you didn't request this, you can ignore this email.");
    }

    /** Sent once an admin approves a pending sign-up, so the applicant knows they can now sign in. */
    @Async
    public void sendAccountApproved(String toEmail, String fullName) {
        trySend(toEmail, "Your Seamline account has been approved",
                "Hi " + fullName + ",\n\n"
                        + "Your Seamline account has been approved. You can now sign in with the email "
                        + "and password (or Google account) you registered with.\n\n"
                        + "If you weren't expecting this, please contact your administrator.");
    }

    private void trySend(String toEmail, String subject, String body) {
        if (!isConfigured()) {
            log.warn("Skipped emailing '{}' to {}: email is not configured", subject, toEmail);
            return;
        }
        SeamlineProperties.Mail mail = properties.mail();

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mail.host());
        sender.setPort(mail.port());
        sender.setUsername(mail.username());
        sender.setPassword(mail.password());
        sender.getJavaMailProperties().put("mail.transport.protocol", "smtp");
        sender.getJavaMailProperties().put("mail.smtp.auth", "true");
        sender.getJavaMailProperties().put("mail.smtp.starttls.enable", "true");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mail.username());
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        try {
            sender.send(message);
            log.info("Emailed '{}' to {}", subject, toEmail);
        } catch (MailException ex) {
            log.warn("Could not email '{}' to {}: {}", subject, toEmail, ex.getMessage());
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
