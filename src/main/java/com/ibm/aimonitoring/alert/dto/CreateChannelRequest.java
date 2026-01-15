package com.ibm.aimonitoring.alert.dto;

import com.ibm.aimonitoring.alert.model.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new notification channel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChannelRequest {
    
    @NotNull(message = "Channel type is required")
    private ChannelType type;
    
    @NotBlank(message = "Channel name is required")
    private String name;
    
    private String description;
    
    @NotBlank(message = "Configuration is required")
    private String configuration;
    
    @Builder.Default
    private boolean enabled = true;
}

// Made with Bob
