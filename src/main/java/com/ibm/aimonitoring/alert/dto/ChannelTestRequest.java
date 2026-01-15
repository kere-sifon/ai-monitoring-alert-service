package com.ibm.aimonitoring.alert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for testing a notification channel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelTestRequest {
    
    private String message;
}

// Made with Bob
