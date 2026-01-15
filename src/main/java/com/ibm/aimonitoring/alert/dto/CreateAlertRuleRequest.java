package com.ibm.aimonitoring.alert.dto;

import com.ibm.aimonitoring.alert.model.RuleType;
import com.ibm.aimonitoring.alert.model.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new alert rule
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAlertRuleRequest {
    
    @NotBlank(message = "Rule name is required")
    private String name;
    
    private String description;
    
    @NotNull(message = "Rule type is required")
    private RuleType type;
    
    @NotNull(message = "Severity is required")
    private Severity severity;
    
    @Builder.Default
    private boolean enabled = true;
    
    private Double anomalyThreshold;
    
    private String conditions;
    
    private String serviceName;
    
    private String logLevel;
    
    private Integer timeWindowMinutes;
    
    private Integer threshold;
    
    @Builder.Default
    private Integer cooldownMinutes = 15;
    
    @Builder.Default
    private boolean notifyOnRecovery = false;
}

// Made with Bob
