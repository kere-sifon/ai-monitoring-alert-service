package com.ibm.aimonitoring.alert.service;

import com.ibm.aimonitoring.alert.model.Alert;
import com.ibm.aimonitoring.alert.model.ChannelType;
import com.ibm.aimonitoring.alert.model.NotificationChannel;
import com.ibm.aimonitoring.alert.repository.NotificationChannelRepository;
import com.ibm.aimonitoring.alert.service.notification.EmailNotificationService;
import com.ibm.aimonitoring.alert.service.notification.NotificationException;
import com.ibm.aimonitoring.alert.service.notification.NotificationService;
import com.ibm.aimonitoring.alert.service.notification.SlackNotificationService;
import com.ibm.aimonitoring.alert.service.notification.WebhookNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Notification Dispatcher Service
 *
 * Orchestrates sending notifications through multiple channels.
 * Handles channel selection, error handling, and async processing.
 */
@Service
@Slf4j
public class NotificationDispatcher {

    private static final String CHANNEL_NOT_FOUND = "Channel not found: ";
    private static final String NO_SERVICE_FOR_CHANNEL_TYPE = "No service found for channel type: ";

    private final NotificationChannelRepository channelRepository;
    private final EmailNotificationService emailService;
    private final SlackNotificationService slackService;
    private final WebhookNotificationService webhookService;

    private final Map<ChannelType, NotificationService> serviceMap;

    public NotificationDispatcher(
            NotificationChannelRepository channelRepository,
            EmailNotificationService emailService,
            SlackNotificationService slackService,
            WebhookNotificationService webhookService) {
        this.channelRepository = channelRepository;
        this.emailService = emailService;
        this.slackService = slackService;
        this.webhookService = webhookService;
        
        // Initialize service map
        this.serviceMap = Map.of(
            ChannelType.EMAIL, emailService,
            ChannelType.SLACK, slackService,
            ChannelType.WEBHOOK, webhookService
        );
    }

    /**
     * Send notifications for an alert through all configured channels
     * 
     * @param alert The alert to send notifications for
     */
    @Async
    public void sendNotifications(Alert alert) {
        log.info("Dispatching notifications for alert ID: {}", alert.getId());
        
        // Get all enabled channels for this alert rule
        List<NotificationChannel> channels = channelRepository
            .findByAlertRuleIdAndEnabledTrue(alert.getAlertRule().getId());
        
        if (channels.isEmpty()) {
            log.warn("No enabled notification channels found for alert rule: {}", 
                alert.getAlertRule().getName());
            return;
        }
        
        int successCount = 0;
        int failureCount = 0;
        
        for (NotificationChannel channel : channels) {
            try {
                sendNotification(alert, channel);
                channel.recordSuccess();
                successCount++;
            } catch (Exception e) {
                log.error("Failed to send notification via channel ID: {}", channel.getId(), e);
                channel.recordFailure();
                failureCount++;
            }
        }
        
        // Save channel statistics
        channelRepository.saveAll(channels);
        
        log.info("Notification dispatch complete for alert ID: {}. Success: {}, Failures: {}", 
            alert.getId(), successCount, failureCount);
    }

    /**
     * Send notification through a specific channel
     * 
     * @param alert The alert to notify about
     * @param channel The notification channel to use
     * @throws NotificationException if notification fails
     */
    public void sendNotification(Alert alert, NotificationChannel channel) throws NotificationException {
        NotificationService service = serviceMap.get(channel.getType());
        
        if (service == null) {
            throw new NotificationException(NO_SERVICE_FOR_CHANNEL_TYPE + channel.getType());
        }
        
        if (!service.isEnabled()) {
            log.debug("Notification service {} is disabled", channel.getType());
            return;
        }
        
        log.debug("Sending notification via {} for alert ID: {}", 
            channel.getType(), alert.getId());
        
        service.sendNotification(alert, channel);
    }

    /**
     * Test a notification channel
     * 
     * @param channel The channel to test
     * @return true if test is successful
     */
    public boolean testChannel(NotificationChannel channel) {
        NotificationService service = serviceMap.get(channel.getType());
        
        if (service == null) {
            log.error(NO_SERVICE_FOR_CHANNEL_TYPE + "{}", channel.getType());
            return false;
        }
        
        if (!service.isEnabled()) {
            log.warn("Notification service {} is disabled", channel.getType());
            return false;
        }
        
        return service.testConnection(channel);
    }

    /**
     * Get notification statistics for a channel
     * 
     * @param channelId The channel ID
     * @return Map with success and failure counts
     */
    public Map<String, Long> getChannelStatistics(Long channelId) {
        NotificationChannel channel = channelRepository.findById(channelId)
            .orElseThrow(() -> new IllegalArgumentException(CHANNEL_NOT_FOUND + channelId));
        
        return Map.of(
            "successCount", channel.getSuccessCount(),
            "failureCount", channel.getFailureCount(),
            "totalCount", channel.getSuccessCount() + channel.getFailureCount()
        );
    }
}

// Made with Bob
