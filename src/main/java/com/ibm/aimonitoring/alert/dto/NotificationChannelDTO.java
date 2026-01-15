package com.ibm.aimonitoring.alert.dto;

import com.ibm.aimonitoring.alert.model.ChannelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for NotificationChannel entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationChannelDTO {
    private Long id;
    private ChannelType type;
    private String name;
    private String description;
    private String configuration;
    private boolean enabled;
    private Long successCount;
    private Long failureCount;
    private LocalDateTime lastSuccessAt;
    private LocalDateTime lastFailureAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

// Made with Bob
