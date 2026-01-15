package com.ibm.aimonitoring.alert.service.notification;

import com.ibm.aimonitoring.alert.model.Alert;
import com.ibm.aimonitoring.alert.model.NotificationChannel;

/**
 * Notification Service Interface
 * 
 * Base interface for all notification channel implementations.
 */
public interface NotificationService {

    /**
     * Check if this notification service is enabled
     */
    boolean isEnabled();

    /**
     * Send notification for an alert
     * 
     * @param alert The alert to notify about
     * @param channel The notification channel configuration
     * @throws NotificationException if notification fails
     */
    void sendNotification(Alert alert, NotificationChannel channel) throws NotificationException;

    /**
     * Test the notification channel connection
     * 
     * @param channel The notification channel to test
     * @return true if connection is successful
     */
    boolean testConnection(NotificationChannel channel);
}

// Made with Bob
