package com.ibm.aimonitoring.alert.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertTest {

    private Alert alert;

    @BeforeEach
    void setUp() {
        alert = Alert.builder()
                .id(1L)
                .status(AlertStatus.OPEN)
                .severity(Severity.HIGH)
                .title("Test Alert")
                .notificationSent(false)
                .notificationFailureCount(0)
                .build();
    }

    @Test
    void acknowledge_shouldSetStatusAndDetails() {
        alert.acknowledge("admin");

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(alert.getAcknowledgedBy()).isEqualTo("admin");
        assertThat(alert.getAcknowledgedAt()).isNotNull();
    }

    @Test
    void resolve_shouldSetStatusAndDetails() {
        alert.resolve("admin", "Fixed the issue");

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(alert.getResolvedBy()).isEqualTo("admin");
        assertThat(alert.getResolutionNotes()).isEqualTo("Fixed the issue");
        assertThat(alert.getResolvedAt()).isNotNull();
    }

    @Test
    void markNotificationSent_shouldUpdateFlags() {
        alert.markNotificationSent();

        assertThat(alert.getNotificationSent()).isTrue();
        assertThat(alert.getNotificationSentAt()).isNotNull();
    }

    @Test
    void recordNotificationFailure_shouldIncrementCountAndSetError() {
        alert.recordNotificationFailure("Connection timeout");

        assertThat(alert.getNotificationFailureCount()).isEqualTo(1);
        assertThat(alert.getLastNotificationError()).isEqualTo("Connection timeout");

        alert.recordNotificationFailure("Another error");
        assertThat(alert.getNotificationFailureCount()).isEqualTo(2);
    }

    @Test
    void isOpen_shouldReturnTrueWhenOpen() {
        assertThat(alert.isOpen()).isTrue();
        assertThat(alert.isAcknowledged()).isFalse();
        assertThat(alert.isResolved()).isFalse();
    }

    @Test
    void isAcknowledged_shouldReturnTrueWhenAcknowledged() {
        alert.setStatus(AlertStatus.ACKNOWLEDGED);

        assertThat(alert.isOpen()).isFalse();
        assertThat(alert.isAcknowledged()).isTrue();
        assertThat(alert.isResolved()).isFalse();
    }

    @Test
    void isResolved_shouldReturnTrueWhenResolved() {
        alert.setStatus(AlertStatus.RESOLVED);

        assertThat(alert.isOpen()).isFalse();
        assertThat(alert.isAcknowledged()).isFalse();
        assertThat(alert.isResolved()).isTrue();
    }
}
