package com.ibm.aimonitoring.alert.controller;

import com.ibm.aimonitoring.alert.model.AlertStatus;
import com.ibm.aimonitoring.alert.model.ChannelType;
import com.ibm.aimonitoring.alert.model.RuleType;
import com.ibm.aimonitoring.alert.model.Severity;
import com.ibm.aimonitoring.alert.repository.AlertRepository;
import com.ibm.aimonitoring.alert.repository.AlertRuleRepository;
import com.ibm.aimonitoring.alert.repository.AnomalyDetectionRepository;
import com.ibm.aimonitoring.alert.repository.NotificationChannelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitoringControllerTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @Mock
    private NotificationChannelRepository channelRepository;

    @Mock
    private AnomalyDetectionRepository anomalyDetectionRepository;

    @InjectMocks
    private MonitoringController monitoringController;

    @Test
    void getSystemHealth_shouldReturnUpStatus() {
        when(alertRuleRepository.count()).thenReturn(5L);
        when(channelRepository.count()).thenReturn(3L);
        when(alertRepository.count()).thenReturn(100L);

        ResponseEntity<?> response = monitoringController.getSystemHealth();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getSystemHealth_shouldReturnDownStatusOnError() {
        when(alertRuleRepository.count()).thenThrow(new RuntimeException("DB error"));

        ResponseEntity<?> response = monitoringController.getSystemHealth();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void getAlertCountsByStatus_shouldReturnCounts() {
        when(alertRepository.countByStatus(AlertStatus.OPEN)).thenReturn(10L);
        when(alertRepository.countByStatus(AlertStatus.ACKNOWLEDGED)).thenReturn(5L);
        when(alertRepository.countByStatus(AlertStatus.RESOLVED)).thenReturn(20L);
        when(alertRepository.countByStatus(AlertStatus.FALSE_POSITIVE)).thenReturn(2L);

        ResponseEntity<Map<String, Long>> response = monitoringController.getAlertCountsByStatus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("OPEN", 10L)
                .containsEntry("RESOLVED", 20L);
    }

    @Test
    void getAlertCountsBySeverity_shouldReturnCounts() {
        when(alertRepository.countBySeverity(Severity.CRITICAL)).thenReturn(5L);
        when(alertRepository.countBySeverity(Severity.HIGH)).thenReturn(10L);
        when(alertRepository.countBySeverity(Severity.MEDIUM)).thenReturn(15L);
        when(alertRepository.countBySeverity(Severity.LOW)).thenReturn(20L);
        when(alertRepository.countBySeverity(Severity.INFO)).thenReturn(8L);

        ResponseEntity<Map<String, Long>> response = monitoringController.getAlertCountsBySeverity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("CRITICAL", 5L)
                .containsEntry("HIGH", 10L);
    }

    @Test
    void getChannelStatistics_shouldReturnStats() {
        when(channelRepository.count()).thenReturn(10L);
        when(channelRepository.countByEnabledTrue()).thenReturn(8L);
        when(channelRepository.countByType(ChannelType.EMAIL)).thenReturn(4L);
        when(channelRepository.countByType(ChannelType.SLACK)).thenReturn(3L);
        when(channelRepository.countByType(ChannelType.WEBHOOK)).thenReturn(3L);

        ResponseEntity<Map<String, Object>> response = monitoringController.getChannelStatistics();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("total_channels", 10L)
                .containsEntry("enabled_channels", 8L);
    }

    @Test
    void getRuleStatistics_shouldReturnStats() {
        when(alertRuleRepository.count()).thenReturn(15L);
        when(alertRuleRepository.countByEnabledTrue()).thenReturn(12L);
        when(alertRuleRepository.countByType(RuleType.ANOMALY_DETECTION)).thenReturn(5L);
        when(alertRuleRepository.countByType(RuleType.THRESHOLD)).thenReturn(6L);
        when(alertRuleRepository.countByType(RuleType.PATTERN_MATCH)).thenReturn(4L);

        ResponseEntity<Map<String, Object>> response = monitoringController.getRuleStatistics();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("total_rules", 15L)
                .containsEntry("enabled_rules", 12L);
    }

    @Test
    void getAlertTrend_shouldReturnHourlyCounts() {
        when(alertRepository.countByCreatedAtBetween(any(), any())).thenReturn(5L);

        ResponseEntity<Map<String, Object>> response = monitoringController.getAlertTrend();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsKey("hourly_counts")
                .containsEntry("period_hours", 24);
    }

    @Test
    void getAnomalyMetrics_shouldReturnMetrics() {
        when(anomalyDetectionRepository.countByDetectedAtAfter(any())).thenReturn(50L);
        when(anomalyDetectionRepository.countByDetectedAtAfterAndConfidenceGreaterThan(any(), anyDouble())).thenReturn(20L);
        when(anomalyDetectionRepository.countUnprocessedAnomalies()).thenReturn(5L);

        ResponseEntity<Map<String, Object>> response = monitoringController.getAnomalyMetrics(24);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("total_anomalies", 50L)
                .containsEntry("high_confidence_anomalies", 20L)
                .containsEntry("period_hours", 24);
    }

    @Test
    void getAlertStatistics_shouldReturnComprehensiveStats() {
        when(alertRepository.countByStatus(any())).thenReturn(10L);
        when(alertRepository.countBySeverity(any())).thenReturn(5L);
        when(alertRepository.countByCreatedAtAfter(any())).thenReturn(25L);
        when(alertRepository.count()).thenReturn(100L);
        when(anomalyDetectionRepository.count()).thenReturn(200L);
        when(anomalyDetectionRepository.countByDetectedAtAfter(any())).thenReturn(50L);
        when(anomalyDetectionRepository.countUnprocessedAnomalies()).thenReturn(10L);
        when(alertRuleRepository.count()).thenReturn(15L);
        when(alertRuleRepository.countByEnabledTrue()).thenReturn(12L);
        when(channelRepository.count()).thenReturn(8L);
        when(channelRepository.countByEnabledTrue()).thenReturn(6L);

        ResponseEntity<?> response = monitoringController.getAlertStatistics(24);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
