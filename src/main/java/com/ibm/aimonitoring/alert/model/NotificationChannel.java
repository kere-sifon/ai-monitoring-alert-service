package com.ibm.aimonitoring.alert.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Notification Channel Entity
 * 
 * Defines how and where alerts should be sent.
 * Supports Email, Slack, and Webhook channels.
 */
@Entity
@Table(name = "notification_channels", schema = "alert_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_rule_id")
    private AlertRule alertRule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChannelType type;
    
    @Column(length = 100)
    private String name;
    
    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    // Channel-specific configuration (stored as JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String config;
    
    // Alternative name for configuration (used by controllers)
    @Column(columnDefinition = "jsonb")
    private String configuration;

    // For EMAIL: recipient email addresses (comma-separated)
    @Column(length = 500)
    private String recipients;

    // For SLACK: channel name or webhook URL
    @Column(name = "slack_channel", length = 200)
    private String slackChannel;

    // For WEBHOOK: endpoint URL
    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    // For WEBHOOK: HTTP method (POST, PUT)
    @Column(name = "webhook_method", length = 10)
    private String webhookMethod;

    // For WEBHOOK: custom headers (stored as JSON)
    @Column(name = "webhook_headers", columnDefinition = "jsonb")
    private String webhookHeaders;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;
    
    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;

    @Column(name = "success_count")
    @Builder.Default
    private Long successCount = 0L;

    @Column(name = "failure_count")
    @Builder.Default
    private Long failureCount = 0L;

    // Helper methods
    public void recordSuccess() {
        this.successCount++;
        this.lastUsedAt = LocalDateTime.now();
        this.lastSuccessAt = LocalDateTime.now();
    }

    public void recordFailure() {
        this.failureCount++;
        this.lastUsedAt = LocalDateTime.now();
        this.lastFailureAt = LocalDateTime.now();
    }
}

// Made with Bob
