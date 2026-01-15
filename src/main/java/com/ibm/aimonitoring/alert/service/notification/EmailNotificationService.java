package com.ibm.aimonitoring.alert.service.notification;

import com.ibm.aimonitoring.alert.model.Alert;
import com.ibm.aimonitoring.alert.model.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;

/**
 * Email Notification Service
 * 
 * Sends alert notifications via email using SMTP.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationService implements NotificationService {

    private final JavaMailSender mailSender;

    @Value("${alert.notification.email.enabled:true}")
    private boolean enabled;

    @Value("${alert.notification.email.from}")
    private String fromEmail;

    @Value("${alert.notification.email.from-name:AI Monitoring System}")
    private String fromName;

    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void sendNotification(Alert alert, NotificationChannel channel) throws NotificationException {
        if (!enabled) {
            log.debug("Email notifications are disabled");
            return;
        }

        if (channel.getRecipients() == null || channel.getRecipients().isEmpty()) {
            throw new NotificationException("No recipients configured for email channel");
        }

        try {
            String[] recipients = channel.getRecipients().split(",");
            for (String recipient : recipients) {
                sendEmail(recipient.trim(), alert);
            }
            log.info("Email notification sent successfully for alert ID: {}", alert.getId());
        } catch (MessagingException | MailException e) {
            log.error("Failed to send email notification for alert ID: {}", alert.getId(), e);
            throw new NotificationException("Failed to send email: " + e.getMessage(), e);
        }
    }

    private void sendEmail(String recipient, Alert alert) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        try {
            helper.setFrom(fromEmail, fromName);
        } catch (java.io.UnsupportedEncodingException e) {
            // Fallback to email without personal name
            helper.setFrom(fromEmail);
            log.warn("Failed to set from name, using email only: {}", e.getMessage());
        }
        helper.setTo(recipient);
        helper.setSubject(buildSubject(alert));
        helper.setText(buildHtmlBody(alert), true);

        mailSender.send(message);
        log.debug("Email sent to: {}", recipient);
    }

    private String buildSubject(Alert alert) {
        return String.format("[%s] %s - %s",
            alert.getSeverity(),
            alert.getAlertRule().getName(),
            alert.getTitle()
        );
    }

    private String buildHtmlBody(Alert alert) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html><head><style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }");
        html.append(".container { max-width: 600px; margin: 0 auto; padding: 20px; }");
        html.append(".header { background-color: ").append(getSeverityColor(alert)).append("; color: white; padding: 15px; border-radius: 5px; }");
        html.append(".content { background-color: #f9f9f9; padding: 20px; margin-top: 20px; border-radius: 5px; }");
        html.append(".field { margin-bottom: 10px; }");
        html.append(".label { font-weight: bold; color: #555; }");
        html.append(".value { color: #333; }");
        html.append(".footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd; font-size: 12px; color: #777; }");
        html.append("</style></head><body>");
        
        html.append("<div class='container'>");
        html.append("<div class='header'>");
        html.append("<h2>").append(alert.getSeverity()).append(" Alert</h2>");
        html.append("<h3>").append(alert.getTitle()).append("</h3>");
        html.append("</div>");
        
        html.append("<div class='content'>");
        
        html.append("<div class='field'>");
        html.append("<span class='label'>Alert Rule:</span> ");
        html.append("<span class='value'>").append(alert.getAlertRule().getName()).append("</span>");
        html.append("</div>");
        
        html.append("<div class='field'>");
        html.append("<span class='label'>Severity:</span> ");
        html.append("<span class='value'>").append(alert.getSeverity()).append("</span>");
        html.append("</div>");
        
        html.append("<div class='field'>");
        html.append("<span class='label'>Status:</span> ");
        html.append("<span class='value'>").append(alert.getStatus()).append("</span>");
        html.append("</div>");
        
        if (alert.getService() != null) {
            html.append("<div class='field'>");
            html.append("<span class='label'>Service:</span> ");
            html.append("<span class='value'>").append(alert.getService()).append("</span>");
            html.append("</div>");
        }
        
        html.append("<div class='field'>");
        html.append("<span class='label'>Created At:</span> ");
        html.append("<span class='value'>").append(alert.getCreatedAt().format(DATE_FORMATTER)).append("</span>");
        html.append("</div>");
        
        if (alert.getDescription() != null && !alert.getDescription().isEmpty()) {
            html.append("<div class='field'>");
            html.append("<span class='label'>Description:</span><br>");
            html.append("<span class='value'>").append(alert.getDescription()).append("</span>");
            html.append("</div>");
        }
        
        if (alert.getAnomalyDetectionId() != null) {
            html.append("<div class='field'>");
            html.append("<span class='label'>Anomaly Detection ID:</span> ");
            html.append("<span class='value'>").append(alert.getAnomalyDetectionId()).append("</span>");
            html.append("</div>");
        }
        
        html.append("</div>");
        
        html.append("<div class='footer'>");
        html.append("<p>This is an automated alert from the AI Log Monitoring System.</p>");
        html.append("<p>Alert ID: ").append(alert.getId()).append("</p>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</body></html>");
        
        return html.toString();
    }

    private String getSeverityColor(Alert alert) {
        return switch (alert.getSeverity()) {
            case CRITICAL -> "#d32f2f";
            case HIGH -> "#f57c00";
            case MEDIUM -> "#fbc02d";
            case LOW -> "#388e3c";
            case INFO -> "#1976d2";
        };
    }

    @Override
    public boolean testConnection(NotificationChannel channel) {
        try {
            // Simple test by checking mail sender configuration
            mailSender.createMimeMessage();
            return true;
        } catch (Exception e) {
            log.error("Email connection test failed", e);
            return false;
        }
    }
}

// Made with Bob
