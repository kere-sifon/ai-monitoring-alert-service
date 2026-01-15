package com.ibm.aimonitoring.alert.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to resolve an alert
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolveAlertRequest {
    
    @NotBlank(message = "resolvedBy is required")
    private String resolvedBy;
    
    private String notes;
}

// Made with Bob
