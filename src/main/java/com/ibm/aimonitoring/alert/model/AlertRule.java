package com.ibm.aimonitoring.alert.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Alert Rule Entity
 * 
 * Defines conditions that trigger alerts when met.
 * Supports multiple rule types and notification channels.
 */
@Entity
@Table(name = "alert_rules", schema = "alert_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RuleType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    // Rule conditions (stored as JSON)
    @Column(columnDefinition = "jsonb")
    private String conditions;

    // Threshold for anomaly confidence (0.0 to 1.0)
    @Column(name = "anomaly_threshold")
    private Double anomalyThreshold;

    // Time window in minutes for threshold-based rules
    @Column(name = "time_window_minutes")
    private Integer timeWindowMinutes;

    // Threshold count for count-based rules (also used as 'threshold' in controllers)
    @Column(name = "threshold_count")
    private Integer thresholdCount;
    
    // Generic threshold field for various rule types
    private Integer threshold;

    // Services to monitor (comma-separated)
    @Column(length = 500)
    private String services;
    
    // Single service name for simpler rules
    @Column(name = "service_name", length = 100)
    private String serviceName;

    // Log levels to monitor (comma-separated)
    @Column(name = "log_levels", length = 100)
    private String logLevels;
    
    // Single log level for simpler rules
    @Column(name = "log_level", length = 20)
    private String logLevel;
    
    // Cooldown period in minutes before re-triggering
    @Column(name = "cooldown_minutes")
    @Builder.Default
    private Integer cooldownMinutes = 15;
    
    // Whether to send notification when alert recovers
    @Column(name = "notify_on_recovery")
    @Builder.Default
    private Boolean notifyOnRecovery = false;

    @OneToMany(mappedBy = "alertRule", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<NotificationChannel> notificationChannels = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @Column(name = "trigger_count")
    @Builder.Default
    private Long triggerCount = 0L;

    // Helper methods
    public void incrementTriggerCount() {
        this.triggerCount++;
        this.lastTriggeredAt = LocalDateTime.now();
    }

    public void addNotificationChannel(NotificationChannel channel) {
        notificationChannels.add(channel);
        channel.setAlertRule(this);
    }

    public void removeNotificationChannel(NotificationChannel channel) {
        notificationChannels.remove(channel);
        channel.setAlertRule(null);
    }
}

// Made with Bob
