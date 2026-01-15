package com.ibm.aimonitoring.alert.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnomalyDetectionTest {

    @Test
    void isHighConfidence_shouldReturnTrueWhenAbove07() {
        AnomalyDetection anomaly = AnomalyDetection.builder()
                .confidence(0.85)
                .build();

        assertThat(anomaly.isHighConfidence()).isTrue();
    }

    @Test
    void isHighConfidence_shouldReturnFalseWhenBelow07() {
        AnomalyDetection anomaly = AnomalyDetection.builder()
                .confidence(0.5)
                .build();

        assertThat(anomaly.isHighConfidence()).isFalse();
    }

    @Test
    void isHighConfidence_shouldReturnFalseWhenNull() {
        AnomalyDetection anomaly = AnomalyDetection.builder()
                .confidence(null)
                .build();

        assertThat(anomaly.isHighConfidence()).isFalse();
    }

    @Test
    void isCriticalAnomaly_shouldReturnTrueWhenAllConditionsMet() {
        AnomalyDetection anomaly = AnomalyDetection.builder()
                .isAnomaly(true)
                .confidence(0.9)
                .anomalyScore(0.85)
                .build();

        assertThat(anomaly.isCriticalAnomaly()).isTrue();
    }

    @Test
    void isCriticalAnomaly_shouldReturnFalseWhenNotAnomaly() {
        AnomalyDetection anomaly = AnomalyDetection.builder()
                .isAnomaly(false)
                .confidence(0.9)
                .anomalyScore(0.85)
                .build();

        assertThat(anomaly.isCriticalAnomaly()).isFalse();
    }

    @Test
    void isCriticalAnomaly_shouldReturnFalseWhenLowConfidence() {
        AnomalyDetection anomaly = AnomalyDetection.builder()
                .isAnomaly(true)
                .confidence(0.5)
                .anomalyScore(0.85)
                .build();

        assertThat(anomaly.isCriticalAnomaly()).isFalse();
    }

    @Test
    void isCriticalAnomaly_shouldReturnFalseWhenLowScore() {
        AnomalyDetection anomaly = AnomalyDetection.builder()
                .isAnomaly(true)
                .confidence(0.9)
                .anomalyScore(0.5)
                .build();

        assertThat(anomaly.isCriticalAnomaly()).isFalse();
    }
}
