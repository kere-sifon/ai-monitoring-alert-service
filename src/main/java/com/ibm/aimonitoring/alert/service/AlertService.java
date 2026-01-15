package com.ibm.aimonitoring.alert.service;

import com.ibm.aimonitoring.alert.model.Alert;
import com.ibm.aimonitoring.alert.model.AlertRule;
import com.ibm.aimonitoring.alert.model.AlertStatus;
import com.ibm.aimonitoring.alert.model.AnomalyDetection;
import com.ibm.aimonitoring.alert.repository.AlertRepository;
import com.ibm.aimonitoring.alert.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Alert Service
 *
 * Core service for alert management including creation,
 * acknowledgment, resolution, and querying.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlertService {

    private static final String ALERT_NOT_FOUND = "Alert not found: ";
    private static final String UNKNOWN_SERVICE = "Unknown Service";

    private final AlertRepository alertRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final RateLimitService rateLimitService;
    private final NotificationDispatcher notificationDispatcher;

    /**
     * Create an alert from an anomaly detection
     * 
     * @param anomaly The anomaly detection
     * @param alertRule The alert rule that triggered
     * @return The created alert
     */
    @Transactional
    public Alert createAlertFromAnomaly(AnomalyDetection anomaly, AlertRule alertRule) {
        log.info("Creating alert for anomaly: {} with rule: {}", 
            anomaly.getLogId(), alertRule.getName());

        // Check rate limiting
        if (!rateLimitService.isAlertAllowed(alertRule)) {
            log.warn("Alert creation blocked by rate limiting for rule: {}", alertRule.getName());
            return null;
        }

        // Build alert
        Alert alert = Alert.builder()
            .alertRule(alertRule)
            .status(AlertStatus.OPEN)
            .severity(alertRule.getSeverity())
            .title(buildAlertTitle(anomaly, alertRule))
            .description(buildAlertDescription(anomaly, alertRule))
            .anomalyDetectionId(anomaly.getLogId())
            .logId(anomaly.getLogId())
            .service(anomaly.getService())
            .notificationSent(false)
            .notificationFailureCount(0)
            .build();

        // Save alert
        alert = alertRepository.save(alert);
        
        // Update rule trigger count
        alertRule.incrementTriggerCount();
        alertRuleRepository.save(alertRule);

        log.info("Alert created with ID: {}", alert.getId());

        // Send notifications asynchronously
        notificationDispatcher.sendNotifications(alert);

        return alert;
    }

    /**
     * Create a manual alert
     * 
     * @param alertRule The alert rule
     * @param title Alert title
     * @param description Alert description
     * @param service Service name
     * @return The created alert
     */
    @Transactional
    public Alert createManualAlert(AlertRule alertRule, String title, 
                                   String description, String service) {
        log.info("Creating manual alert with rule: {}", alertRule.getName());

        Alert alert = Alert.builder()
            .alertRule(alertRule)
            .status(AlertStatus.OPEN)
            .severity(alertRule.getSeverity())
            .title(title)
            .description(description)
            .service(service)
            .notificationSent(false)
            .notificationFailureCount(0)
            .build();

        alert = alertRepository.save(alert);
        
        alertRule.incrementTriggerCount();
        alertRuleRepository.save(alertRule);

        log.info("Manual alert created with ID: {}", alert.getId());

        notificationDispatcher.sendNotifications(alert);

        return alert;
    }

    /**
     * Acknowledge an alert
     * 
     * @param alertId The alert ID
     * @param acknowledgedBy Who acknowledged the alert
     * @return The updated alert
     */
    @Transactional
    public Alert acknowledgeAlert(Long alertId, String acknowledgedBy) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException(ALERT_NOT_FOUND + alertId));

        if (alert.getStatus() != AlertStatus.OPEN) {
            throw new IllegalStateException("Only OPEN alerts can be acknowledged");
        }

        alert.acknowledge(acknowledgedBy);
        alert = alertRepository.save(alert);

        log.info("Alert {} acknowledged by {}", alertId, acknowledgedBy);

        return alert;
    }

    /**
     * Resolve an alert
     * 
     * @param alertId The alert ID
     * @param resolvedBy Who resolved the alert
     * @param notes Resolution notes
     * @return The updated alert
     */
    @Transactional
    public Alert resolveAlert(Long alertId, String resolvedBy, String notes) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException(ALERT_NOT_FOUND + alertId));

        if (alert.getStatus() == AlertStatus.RESOLVED) {
            throw new IllegalStateException("Alert is already resolved");
        }

        alert.resolve(resolvedBy, notes);
        alert = alertRepository.save(alert);

        log.info("Alert {} resolved by {}", alertId, resolvedBy);

        return alert;
    }

    /**
     * Mark alert as false positive
     * 
     * @param alertId The alert ID
     * @param markedBy Who marked it as false positive
     * @return The updated alert
     */
    @Transactional
    public Alert markAsFalsePositive(Long alertId, String markedBy) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException(ALERT_NOT_FOUND + alertId));

        alert.setStatus(AlertStatus.FALSE_POSITIVE);
        alert.setResolvedBy(markedBy);
        alert.setResolvedAt(LocalDateTime.now());
        alert = alertRepository.save(alert);

        log.info("Alert {} marked as false positive by {}", alertId, markedBy);

        return alert;
    }

    /**
     * Get alert by ID
     * 
     * @param alertId The alert ID
     * @return The alert
     */
    public Alert getAlert(Long alertId) {
        return alertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException(ALERT_NOT_FOUND + alertId));
    }

    /**
     * Get all alerts with pagination
     * 
     * @param pageable Pagination parameters
     * @return Page of alerts
     */
    public Page<Alert> getAllAlerts(Pageable pageable) {
        return alertRepository.findAll(pageable);
    }

    /**
     * Get alerts by status
     * 
     * @param status The alert status
     * @param pageable Pagination parameters
     * @return Page of alerts
     */
    public Page<Alert> getAlertsByStatus(AlertStatus status, Pageable pageable) {
        return alertRepository.findByStatus(status, pageable);
    }

    /**
     * Get open alerts
     * 
     * @return List of open alerts
     */
    public List<Alert> getOpenAlerts() {
        return alertRepository.findByStatus(AlertStatus.OPEN);
    }

    /**
     * Get recent alerts
     * 
     * @param minutes Number of minutes to look back
     * @param pageable Pagination parameters
     * @return List of recent alerts
     */
    public List<Alert> getRecentAlerts(int minutes, Pageable pageable) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return alertRepository.findRecentAlerts(since, pageable);
    }

    /**
     * Get alerts for a specific service
     * 
     * @param service The service name
     * @param pageable Pagination parameters
     * @return Page of alerts
     */
    public Page<Alert> getAlertsByService(String service, Pageable pageable) {
        return alertRepository.findByService(service, pageable);
    }

    /**
     * Get alert statistics
     * 
     * @return Map with alert counts by status
     */
    public java.util.Map<String, Long> getAlertStatistics() {
        return java.util.Map.of(
            "open", alertRepository.countByStatus(AlertStatus.OPEN),
            "acknowledged", alertRepository.countByStatus(AlertStatus.ACKNOWLEDGED),
            "resolved", alertRepository.countByStatus(AlertStatus.RESOLVED),
            "falsePositive", alertRepository.countByStatus(AlertStatus.FALSE_POSITIVE),
            "total", alertRepository.count()
        );
    }

    /**
     * Retry failed notifications for an alert
     * 
     * @param alertId The alert ID
     */
    @Transactional
    public void retryNotifications(Long alertId) {
        Alert alert = getAlert(alertId);
        
        if (alert.getNotificationSent()) {
            log.warn("Notifications already sent for alert {}", alertId);
            return;
        }

        log.info("Retrying notifications for alert {}", alertId);
        notificationDispatcher.sendNotifications(alert);
    }

    // Helper methods

    private String buildAlertTitle(AnomalyDetection anomaly, AlertRule alertRule) {
        return String.format("Anomaly Detected: %s - %s",
            alertRule.getName(),
            anomaly.getService() != null ? anomaly.getService() : UNKNOWN_SERVICE
        );
    }

    private String buildAlertDescription(AnomalyDetection anomaly, AlertRule alertRule) {
        StringBuilder desc = new StringBuilder();
        desc.append("An anomaly was detected by the ML service.\n\n");
        desc.append(String.format("Confidence: %.2f%%\n", anomaly.getConfidence() * 100));
        desc.append(String.format("Anomaly Score: %.2f\n", anomaly.getAnomalyScore()));
        
        if (anomaly.getMessage() != null) {
            desc.append(String.format("\nLog Message: %s\n", anomaly.getMessage()));
        }
        
        if (anomaly.getLevel() != null) {
            desc.append(String.format("Log Level: %s\n", anomaly.getLevel()));
        }
        
        desc.append(String.format("\nDetected At: %s", anomaly.getDetectedAt()));
        
        return desc.toString();
    }
}

// Made with Bob
