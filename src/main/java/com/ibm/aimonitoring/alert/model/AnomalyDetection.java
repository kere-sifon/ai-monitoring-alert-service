package com.ibm.aimonitoring.alert.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Anomaly Detection Entity (Read-only)
 * 
 * Maps to the anomaly_detections table in ml_service schema.
 * Used by Alert Service to monitor for new anomalies.
 */
@Entity
@Table(name = "anomaly_detections", schema = "ml_service")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyDetection {

    @Id
    @Column(name = "log_id")
    private String logId;

    @Column(name = "is_anomaly", nullable = false)
    private Boolean isAnomaly;

    @Column(name = "anomaly_score", nullable = false)
    private Double anomalyScore;

    @Column(nullable = false)
    private Double confidence;

    @Column(name = "model_version", length = 50)
    private String modelVersion;

    @Column(columnDefinition = "jsonb")
    private String features;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    // Log entry details (denormalized for convenience)
    @Column(length = 20)
    private String level;

    @Column(columnDefinition = "text")
    private String message;

    @Column(length = 100)
    private String service;

    @Column(name = "log_timestamp")
    private LocalDateTime logTimestamp;

    // Helper methods
    public boolean isHighConfidence() {
        return confidence != null && confidence > 0.7;
    }

    public boolean isCriticalAnomaly() {
        return isAnomaly && isHighConfidence() && anomalyScore > 0.8;
    }
}

// Made with Bob
