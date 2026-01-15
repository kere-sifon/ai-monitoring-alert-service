package com.ibm.aimonitoring.alert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for system health status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthDTO {
    private String status;
    private LocalDateTime timestamp;
    private String database;
    private String scheduler;
    private Long totalAlertRules;
    private Long totalNotificationChannels;
    private Long totalAlerts;
    private String error;
}

// Made with Bob
