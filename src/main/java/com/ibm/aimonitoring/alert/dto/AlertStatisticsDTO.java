package com.ibm.aimonitoring.alert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for comprehensive alert statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertStatisticsDTO {
    private LocalDateTime timestamp;
    private int timeRangeHours;
    
    // Alert counts by status
    private long totalAlerts;
    private long openAlerts;
    private long acknowledgedAlerts;
    private long resolvedAlerts;
    private long falsePositiveAlerts;
    
    // Alert counts by severity
    private long criticalAlerts;
    private long highAlerts;
    private long mediumAlerts;
    private long lowAlerts;
    
    // Recent activity
    private long recentAlerts;
    
    // Anomaly statistics
    private long totalAnomalies;
    private long recentAnomalies;
    private long unprocessedAnomalies;
    
    // Rule statistics
    private long totalAlertRules;
    private long enabledAlertRules;
    
    // Channel statistics
    private long totalNotificationChannels;
    private long enabledNotificationChannels;
}

// Made with Bob
