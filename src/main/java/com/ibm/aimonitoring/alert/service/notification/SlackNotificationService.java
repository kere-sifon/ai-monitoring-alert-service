package com.ibm.aimonitoring.alert.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.aimonitoring.alert.model.Alert;
import com.ibm.aimonitoring.alert.model.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Slack Notification Service
 * 
 * Sends alert notifications to Slack via webhooks.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SlackNotificationService implements NotificationService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${alert.notification.slack.enabled:true}")
    private boolean enabled;

    @Value("${alert.notification.slack.timeout:5000}")
    private int timeout;

    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void sendNotification(Alert alert, NotificationChannel channel) throws NotificationException {
        if (!enabled) {
            log.debug("Slack notifications are disabled");
            return;
        }

        if (channel.getSlackChannel() == null || channel.getSlackChannel().isEmpty()) {
            throw new NotificationException("No Slack webhook URL configured");
        }

        try {
            String webhookUrl = channel.getSlackChannel();
            Map<String, Object> payload = buildSlackPayload(alert);
            
            WebClient webClient = webClientBuilder
                .baseUrl(webhookUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

            String response = webClient.post()
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeout))
                .block();

            log.info("Slack notification sent successfully for alert ID: {}", alert.getId());
        } catch (Exception e) {
            log.error("Failed to send Slack notification for alert ID: {}", alert.getId(), e);
            throw new NotificationException("Failed to send Slack notification: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildSlackPayload(Alert alert) {
        Map<String, Object> payload = new HashMap<>();
        
        // Build blocks for rich formatting
        payload.put("blocks", buildBlocks(alert));
        
        // Fallback text for notifications
        payload.put("text", String.format("[%s] %s - %s", 
            alert.getSeverity(), 
            alert.getAlertRule().getName(), 
            alert.getTitle()
        ));
        
        return payload;
    }

    private Object[] buildBlocks(Alert alert) {
        return new Object[] {
            // Header block
            Map.of(
                "type", "header",
                "text", Map.of(
                    "type", "plain_text",
                    "text", String.format("🚨 %s Alert", alert.getSeverity()),
                    "emoji", true
                )
            ),
            
            // Title section
            Map.of(
                "type", "section",
                "text", Map.of(
                    "type", "mrkdwn",
                    "text", String.format("*%s*\n%s", 
                        alert.getTitle(),
                        alert.getDescription() != null ? alert.getDescription() : ""
                    )
                )
            ),
            
            // Divider
            Map.of("type", "divider"),
            
            // Details section
            Map.of(
                "type", "section",
                "fields", new Object[] {
                    Map.of(
                        "type", "mrkdwn",
                        "text", String.format("*Alert Rule:*\n%s", alert.getAlertRule().getName())
                    ),
                    Map.of(
                        "type", "mrkdwn",
                        "text", String.format("*Severity:*\n%s %s", 
                            getSeverityEmoji(alert), 
                            alert.getSeverity()
                        )
                    ),
                    Map.of(
                        "type", "mrkdwn",
                        "text", String.format("*Status:*\n%s", alert.getStatus())
                    ),
                    Map.of(
                        "type", "mrkdwn",
                        "text", String.format("*Service:*\n%s", 
                            alert.getService() != null ? alert.getService() : "N/A"
                        )
                    ),
                    Map.of(
                        "type", "mrkdwn",
                        "text", String.format("*Created:*\n%s", 
                            alert.getCreatedAt().format(DATE_FORMATTER)
                        )
                    ),
                    Map.of(
                        "type", "mrkdwn",
                        "text", String.format("*Alert ID:*\n%s", alert.getId())
                    )
                }
            ),
            
            // Context
            Map.of(
                "type", "context",
                "elements", new Object[] {
                    Map.of(
                        "type", "mrkdwn",
                        "text", "AI Log Monitoring System"
                    )
                }
            )
        };
    }

    private String getSeverityEmoji(Alert alert) {
        return switch (alert.getSeverity()) {
            case CRITICAL -> "🔴";
            case HIGH -> "🟠";
            case MEDIUM -> "🟡";
            case LOW -> "🟢";
            case INFO -> "🔵";
        };
    }

    @Override
    public boolean testConnection(NotificationChannel channel) {
        try {
            if (channel.getSlackChannel() == null || channel.getSlackChannel().isEmpty()) {
                return false;
            }

            // Send a simple test message
            Map<String, Object> testPayload = Map.of(
                "text", "Test message from AI Monitoring System"
            );

            WebClient webClient = webClientBuilder
                .baseUrl(channel.getSlackChannel())
                .build();

            String response = webClient.post()
                .bodyValue(testPayload)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeout))
                .block();

            return "ok".equals(response);
        } catch (Exception e) {
            log.error("Slack connection test failed", e);
            return false;
        }
    }
}

// Made with Bob
