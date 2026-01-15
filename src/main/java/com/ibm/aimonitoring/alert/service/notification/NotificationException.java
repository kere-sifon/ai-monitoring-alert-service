package com.ibm.aimonitoring.alert.service.notification;

/**
 * Exception thrown when notification sending fails
 */
public class NotificationException extends Exception {

    public NotificationException(String message) {
        super(message);
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Made with Bob
