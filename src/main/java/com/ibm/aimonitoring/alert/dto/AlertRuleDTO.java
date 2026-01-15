package com.ibm.aimonitoring.alert.dto;

import com.ibm.aimonitoring.alert.model.RuleType;
import com.ibm.aimonitoring.alert.model.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for AlertRule entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleDTO {
    private Long id;
    private String name;
    private String description;
    private RuleType type;
    private Severity severity;
    private boolean enabled;
    private Double anomalyThreshold;
    private String conditions;
    private String serviceName;
    private String logLevel;
    private Integer timeWindowMinutes;
    private Integer threshold;
    private Integer cooldownMinutes;
    private boolean notifyOnRecovery;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// Made with Bob
