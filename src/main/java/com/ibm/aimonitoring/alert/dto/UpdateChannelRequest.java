package com.ibm.aimonitoring.alert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating an existing notification channel
 * All fields are optional - only provided fields will be updated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChannelRequest {
    
    private String name;
    
    private String description;
    
    private String configuration;
    
    private Boolean enabled;
}

// Made with Bob
