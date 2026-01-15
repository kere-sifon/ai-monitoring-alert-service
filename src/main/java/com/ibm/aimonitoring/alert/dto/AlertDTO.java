package com.ibm.aimonitoring.alert.dto;

import com.ibm.aimonitoring.alert.model.AlertStatus;
import com.ibm.aimonitoring.alert.model.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Alert Data Transfer Object
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertDTO {
    private Long id;
    private Long alertRuleId;
    private String alertRuleName;
    private AlertStatus status;
    private Severity severity;
    private String title;
    private String description;
    private String anomalyDetectionId;
    private String logId;
    private String service;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime acknowledgedAt;
    private String acknowledgedBy;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private String resolutionNotes;
    private Boolean notificationSent;
    private LocalDateTime notificationSentAt;
    private Integer notificationFailureCount;
}

// Made with Bob
