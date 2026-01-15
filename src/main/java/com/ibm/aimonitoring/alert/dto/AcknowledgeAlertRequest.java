package com.ibm.aimonitoring.alert.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to acknowledge an alert
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcknowledgeAlertRequest {
    
    @NotBlank(message = "acknowledgedBy is required")
    private String acknowledgedBy;
}

// Made with Bob
