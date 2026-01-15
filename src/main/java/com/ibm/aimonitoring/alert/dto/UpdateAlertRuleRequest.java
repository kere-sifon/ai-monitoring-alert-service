package com.ibm.aimonitoring.alert.dto;

import com.ibm.aimonitoring.alert.model.RuleType;
import com.ibm.aimonitoring.alert.model.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing alert rule
 * All fields are optional - only provided fields will be updated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAlertRuleRequest {
    
    private String name;
    
    private String description;
    
    private RuleType type;
    
    private Severity severity;
    
    private Double anomalyThreshold;
    
    private String conditions;
    
    private String serviceName;
    
    private String logLevel;
    
    private Integer timeWindowMinutes;
    
    private Integer threshold;
    
    private Integer cooldownMinutes;
    
    private Boolean notifyOnRecovery;
}

// Made with Bob
