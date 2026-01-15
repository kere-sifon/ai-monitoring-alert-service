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
 * Alert Entity
 * 
 * Represents a triggered alert instance.
 * Tracks alert lifecycle from creation to resolution.
 */
@Entity
@Table(name = "alerts", schema = "alert_service", indexes = {
    @Index(name = "idx_alert_rule_id", columnList = "alert_rule_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_anomaly_id", columnList = "anomaly_detection_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_rule_id", nullable = false)
    private AlertRule alertRule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    // Reference to the anomaly detection that triggered this alert
    @Column(name = "anomaly_detection_id")
    private String anomalyDetectionId;

    // Reference to the log entry
    @Column(name = "log_id")
    private String logId;

    // Service that generated the log
    @Column(length = 100)
    private String service;

    // Additional context (stored as JSON)
    @Column(columnDefinition = "jsonb")
    private String context;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by", length = 100)
    private String resolvedBy;

    @Column(name = "resolution_notes", columnDefinition = "text")
    private String resolutionNotes;

    @Column(name = "notification_sent")
    @Builder.Default
    private Boolean notificationSent = false;

    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;

    @Column(name = "notification_failure_count")
    @Builder.Default
    private Integer notificationFailureCount = 0;

    @Column(name = "last_notification_error", columnDefinition = "text")
    private String lastNotificationError;

    // Helper methods
    public void acknowledge(String acknowledgedBy) {
        this.status = AlertStatus.ACKNOWLEDGED;
        this.acknowledgedAt = LocalDateTime.now();
        this.acknowledgedBy = acknowledgedBy;
    }

    public void resolve(String resolvedBy, String notes) {
        this.status = AlertStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = resolvedBy;
        this.resolutionNotes = notes;
    }

    public void markNotificationSent() {
        this.notificationSent = true;
        this.notificationSentAt = LocalDateTime.now();
    }

    public void recordNotificationFailure(String error) {
        this.notificationFailureCount++;
        this.lastNotificationError = error;
    }

    public boolean isOpen() {
        return status == AlertStatus.OPEN;
    }

    public boolean isAcknowledged() {
        return status == AlertStatus.ACKNOWLEDGED;
    }

    public boolean isResolved() {
        return status == AlertStatus.RESOLVED;
    }
}

// Made with Bob
