package com.ibm.aimonitoring.alert.service.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.aimonitoring.alert.model.Alert;
import com.ibm.aimonitoring.alert.model.NotificationChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Webhook Notification Service
 * 
 * Sends alert notifications to generic HTTP webhooks.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebhookNotificationService implements NotificationService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${alert.notification.webhook.enabled:true}")
    private boolean enabled;

    @Value("${alert.notification.webhook.timeout:10000}")
    private int timeout;

    @Value("${alert.notification.webhook.retry-on-failure:true}")
    private boolean retryOnFailure;

    @Value("${alert.notification.retry.max-attempts:3}")
    private int maxRetries;

    @Value("${alert.notification.retry.backoff-delay:2000}")
    private long backoffDelay;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void sendNotification(Alert alert, NotificationChannel channel) throws NotificationException {
        if (!enabled) {
            log.debug("Webhook notifications are disabled");
            return;
        }

        if (channel.getWebhookUrl() == null || channel.getWebhookUrl().isEmpty()) {
            throw new NotificationException("No webhook URL configured");
        }

        try {
            String webhookUrl = channel.getWebhookUrl();
            String method = channel.getWebhookMethod() != null ? 
                channel.getWebhookMethod().toUpperCase() : "POST";
            
            Map<String, Object> payload = buildWebhookPayload(alert);
            Map<String, String> customHeaders = parseCustomHeaders(channel.getWebhookHeaders());
            
            WebClient webClient = buildWebClient(webhookUrl, customHeaders);
            
            Mono<String> request = executeRequest(webClient, method, payload);
            
            if (retryOnFailure) {
                request = request.retryWhen(Retry.backoff(maxRetries, Duration.ofMillis(backoffDelay))
                    .doBeforeRetry(retrySignal -> 
                        log.warn("Retrying webhook notification, attempt: {}", retrySignal.totalRetries() + 1)
                    )
                );
            }
            
            String response = request
                .timeout(Duration.ofMillis(timeout))
                .block();

            log.info("Webhook notification sent successfully for alert ID: {} to {}", 
                alert.getId(), webhookUrl);
        } catch (Exception e) {
            log.error("Failed to send webhook notification for alert ID: {}", alert.getId(), e);
            throw new NotificationException("Failed to send webhook notification: " + e.getMessage(), e);
        }
    }

    private WebClient buildWebClient(String baseUrl, Map<String, String> customHeaders) {
        WebClient.Builder builder = webClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        
        // Add custom headers
        if (customHeaders != null && !customHeaders.isEmpty()) {
            customHeaders.forEach(builder::defaultHeader);
        }
        
        return builder.build();
    }

    private Mono<String> executeRequest(WebClient webClient, String method, Map<String, Object> payload) {
        return switch (method) {
            case "POST" -> webClient.post()
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class);
            case "PUT" -> webClient.put()
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class);
            case "PATCH" -> webClient.patch()
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class);
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };
    }

    private Map<String, Object> buildWebhookPayload(Alert alert) {
        Map<String, Object> payload = new HashMap<>();
        
        // Alert basic info
        payload.put("alert_id", alert.getId());
        payload.put("title", alert.getTitle());
        payload.put("description", alert.getDescription());
        payload.put("severity", alert.getSeverity().toString());
        payload.put("status", alert.getStatus().toString());
        payload.put("created_at", alert.getCreatedAt().toString());
        
        // Alert rule info
        Map<String, Object> ruleInfo = new HashMap<>();
        ruleInfo.put("id", alert.getAlertRule().getId());
        ruleInfo.put("name", alert.getAlertRule().getName());
        ruleInfo.put("type", alert.getAlertRule().getType().toString());
        payload.put("alert_rule", ruleInfo);
        
        // Service info
        if (alert.getService() != null) {
            payload.put("service", alert.getService());
        }
        
        // Anomaly info
        if (alert.getAnomalyDetectionId() != null) {
            Map<String, Object> anomalyInfo = new HashMap<>();
            anomalyInfo.put("detection_id", alert.getAnomalyDetectionId());
            anomalyInfo.put("log_id", alert.getLogId());
            payload.put("anomaly", anomalyInfo);
        }
        
        // Context (if available)
        if (alert.getContext() != null) {
            try {
                Map<String, Object> context = objectMapper.readValue(
                    alert.getContext(), 
                    new TypeReference<Map<String, Object>>() {}
                );
                payload.put("context", context);
            } catch (Exception e) {
                log.warn("Failed to parse alert context JSON", e);
            }
        }
        
        // Metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "ai-monitoring-system");
        metadata.put("version", "1.0.0");
        metadata.put("timestamp", System.currentTimeMillis());
        payload.put("metadata", metadata);
        
        return payload;
    }

    private Map<String, String> parseCustomHeaders(String headersJson) {
        if (headersJson == null || headersJson.isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(headersJson, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse custom headers JSON, using empty headers", e);
            return new HashMap<>();
        }
    }

    @Override
    public boolean testConnection(NotificationChannel channel) {
        try {
            if (channel.getWebhookUrl() == null || channel.getWebhookUrl().isEmpty()) {
                return false;
            }

            // Send a simple test payload
            Map<String, Object> testPayload = Map.of(
                "test", true,
                "message", "Test notification from AI Monitoring System",
                "timestamp", System.currentTimeMillis()
            );

            Map<String, String> customHeaders = parseCustomHeaders(channel.getWebhookHeaders());
            WebClient webClient = buildWebClient(channel.getWebhookUrl(), customHeaders);
            
            String method = channel.getWebhookMethod() != null ? 
                channel.getWebhookMethod().toUpperCase() : "POST";

            String response = executeRequest(webClient, method, testPayload)
                .timeout(Duration.ofMillis(timeout))
                .block();

            return response != null;
        } catch (Exception e) {
            log.error("Webhook connection test failed", e);
            return false;
        }
    }
}

// Made with Bob
