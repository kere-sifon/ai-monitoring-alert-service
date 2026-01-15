package com.ibm.aimonitoring.alert.service;

import com.ibm.aimonitoring.alert.model.AlertRule;
import com.ibm.aimonitoring.alert.model.AnomalyDetection;
import com.ibm.aimonitoring.alert.repository.AnomalyDetectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnomalyMonitoringSchedulerTest {

    @Mock
    private AnomalyDetectionRepository anomalyDetectionRepository;

    @Mock
    private AlertRuleEngine alertRuleEngine;

    @InjectMocks
    private AnomalyMonitoringScheduler scheduler;

    private AnomalyDetection testAnomaly;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "enabled", true);
        ReflectionTestUtils.setField(scheduler, "lookbackMinutes", 5);
        ReflectionTestUtils.setField(scheduler, "batchSize", 100);

        testAnomaly = new AnomalyDetection();
        testAnomaly.setLogId("log-123");
        testAnomaly.setConfidence(0.95);
        testAnomaly.setAnomalyScore(0.9);
        testAnomaly.setDetectedAt(LocalDateTime.now());
    }

    @Test
    void monitorAnomalies_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "enabled", false);

        scheduler.monitorAnomalies();

        verifyNoInteractions(anomalyDetectionRepository);
        verifyNoInteractions(alertRuleEngine);
    }

    @Test
    void monitorAnomalies_shouldProcessUnprocessedAnomalies() {
        when(anomalyDetectionRepository.findUnprocessedAnomalies(any(LocalDateTime.class)))
                .thenReturn(List.of(testAnomaly));
        when(alertRuleEngine.evaluateAnomalyRules(any(AnomalyDetection.class)))
                .thenReturn(List.of(new AlertRule()));

        scheduler.monitorAnomalies();

        verify(anomalyDetectionRepository).findUnprocessedAnomalies(any(LocalDateTime.class));
        verify(alertRuleEngine).evaluateAnomalyRules(testAnomaly);
    }

    @Test
    void monitorAnomalies_shouldHandleNoAnomalies() {
        when(anomalyDetectionRepository.findUnprocessedAnomalies(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        scheduler.monitorAnomalies();

        verify(anomalyDetectionRepository).findUnprocessedAnomalies(any(LocalDateTime.class));
        verifyNoInteractions(alertRuleEngine);
    }

    @Test
    void monitorAnomalies_shouldHandleException() {
        when(anomalyDetectionRepository.findUnprocessedAnomalies(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        scheduler.monitorAnomalies();

        verify(anomalyDetectionRepository).findUnprocessedAnomalies(any(LocalDateTime.class));
        verifyNoInteractions(alertRuleEngine);
    }

    @Test
    void monitorCriticalAnomalies_shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "enabled", false);

        scheduler.monitorCriticalAnomalies();

        verifyNoInteractions(anomalyDetectionRepository);
    }

    @Test
    void monitorCriticalAnomalies_shouldProcessCriticalAnomalies() {
        when(anomalyDetectionRepository.findCriticalAnomalies(anyDouble(), anyDouble(), any(LocalDateTime.class)))
                .thenReturn(List.of(testAnomaly));
        when(alertRuleEngine.evaluateAnomalyRules(any(AnomalyDetection.class)))
                .thenReturn(List.of(new AlertRule()));

        scheduler.monitorCriticalAnomalies();

        verify(anomalyDetectionRepository).findCriticalAnomalies(anyDouble(), anyDouble(), any(LocalDateTime.class));
        verify(alertRuleEngine).evaluateAnomalyRules(testAnomaly);
    }

    @Test
    void triggerManualCheck_shouldCallMonitorAnomalies() {
        when(anomalyDetectionRepository.findUnprocessedAnomalies(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        scheduler.triggerManualCheck();

        verify(anomalyDetectionRepository).findUnprocessedAnomalies(any(LocalDateTime.class));
    }

    @Test
    void getMonitoringStatus_shouldReturnStatus() {
        Map<String, Object> status = scheduler.getMonitoringStatus();

        assertThat(status)
                .containsEntry("enabled", true)
                .containsEntry("lookbackMinutes", 5)
                .containsEntry("batchSize", 100)
                .containsKey("lastCheckTime");
    }

    @Test
    void getAnomalyStatistics_shouldReturnStatistics() {
        when(anomalyDetectionRepository.countAnomaliesBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(10L);
        when(anomalyDetectionRepository.findUnprocessedAnomalies(any(LocalDateTime.class)))
                .thenReturn(List.of(testAnomaly));
        when(anomalyDetectionRepository.findCriticalAnomalies(anyDouble(), anyDouble(), any(LocalDateTime.class)))
                .thenReturn(List.of(testAnomaly));

        Map<String, Object> stats = scheduler.getAnomalyStatistics();

        assertThat(stats)
                .containsEntry("totalAnomalies", 10L)
                .containsEntry("unprocessedCount", 1)
                .containsEntry("criticalCount", 1)
                .containsKey("timeWindow")
                .containsKey("lastCheckTime");
    }

    @Test
    void resetLastCheckTime_shouldResetTime() {
        scheduler.resetLastCheckTime();

        Map<String, Object> status = scheduler.getMonitoringStatus();
        assertThat(status).containsKey("lastCheckTime");
    }

    @Test
    void monitorAnomalies_shouldRespectBatchSizeLimit() {
        ReflectionTestUtils.setField(scheduler, "batchSize", 2);
        
        AnomalyDetection anomaly2 = new AnomalyDetection();
        anomaly2.setLogId("log-456");
        AnomalyDetection anomaly3 = new AnomalyDetection();
        anomaly3.setLogId("log-789");

        when(anomalyDetectionRepository.findUnprocessedAnomalies(any(LocalDateTime.class)))
                .thenReturn(List.of(testAnomaly, anomaly2, anomaly3));
        when(alertRuleEngine.evaluateAnomalyRules(any(AnomalyDetection.class)))
                .thenReturn(Collections.emptyList());

        scheduler.monitorAnomalies();

        // Should only process 2 anomalies due to batch size limit
        verify(alertRuleEngine, times(2)).evaluateAnomalyRules(any(AnomalyDetection.class));
    }

    @Test
    void monitorCriticalAnomalies_shouldHandleException() {
        when(anomalyDetectionRepository.findCriticalAnomalies(anyDouble(), anyDouble(), any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        scheduler.monitorCriticalAnomalies();

        verify(anomalyDetectionRepository).findCriticalAnomalies(anyDouble(), anyDouble(), any(LocalDateTime.class));
    }

    @Test
    void monitorCriticalAnomalies_shouldHandleEmptyList() {
        when(anomalyDetectionRepository.findCriticalAnomalies(anyDouble(), anyDouble(), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        scheduler.monitorCriticalAnomalies();

        verifyNoInteractions(alertRuleEngine);
    }

    @Test
    void monitorAnomalies_shouldHandleExceptionInProcessing() {
        when(anomalyDetectionRepository.findUnprocessedAnomalies(any(LocalDateTime.class)))
                .thenReturn(List.of(testAnomaly));
        when(alertRuleEngine.evaluateAnomalyRules(any(AnomalyDetection.class)))
                .thenThrow(new RuntimeException("Processing error"));

        scheduler.monitorAnomalies();

        verify(alertRuleEngine).evaluateAnomalyRules(testAnomaly);
    }
}
