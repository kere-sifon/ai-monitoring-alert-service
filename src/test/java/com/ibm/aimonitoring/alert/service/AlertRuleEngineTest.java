package com.ibm.aimonitoring.alert.service;

import com.ibm.aimonitoring.alert.model.AlertRule;
import com.ibm.aimonitoring.alert.model.AnomalyDetection;
import com.ibm.aimonitoring.alert.model.RuleType;
import com.ibm.aimonitoring.alert.model.Severity;
import com.ibm.aimonitoring.alert.repository.AlertRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertRuleEngineTest {

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @Mock
    private AlertService alertService;

    @InjectMocks
    private AlertRuleEngine alertRuleEngine;

    private AlertRule testRule;
    private AnomalyDetection testAnomaly;

    @BeforeEach
    void setUp() {
        testRule = AlertRule.builder()
                .id(1L)
                .name("Test Rule")
                .type(RuleType.ANOMALY_DETECTION)
                .severity(Severity.HIGH)
                .enabled(true)
                .anomalyThreshold(0.7)
                .build();

        testAnomaly = AnomalyDetection.builder()
                .logId("log-123")
                .isAnomaly(true)
                .confidence(0.85)
                .anomalyScore(0.9)
                .service("test-service")
                .level("ERROR")
                .detectedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void evaluateAnomalyRules_shouldTriggerRule_whenAnomalyMeetsThreshold() {
        when(alertRuleRepository.findByTypeAndEnabledTrue(RuleType.ANOMALY_DETECTION))
                .thenReturn(List.of(testRule));

        List<AlertRule> triggeredRules = alertRuleEngine.evaluateAnomalyRules(testAnomaly);

        assertThat(triggeredRules).hasSize(1);
        assertThat(triggeredRules.get(0).getName()).isEqualTo("Test Rule");
        verify(alertService).createAlertFromAnomaly(testAnomaly, testRule);
    }

    @Test
    void evaluateAnomalyRules_shouldNotTrigger_whenConfidenceBelowThreshold() {
        testAnomaly.setConfidence(0.5);
        when(alertRuleRepository.findByTypeAndEnabledTrue(RuleType.ANOMALY_DETECTION))
                .thenReturn(List.of(testRule));

        List<AlertRule> triggeredRules = alertRuleEngine.evaluateAnomalyRules(testAnomaly);

        assertThat(triggeredRules).isEmpty();
        verify(alertService, never()).createAlertFromAnomaly(any(), any());
    }

    @Test
    void evaluateAnomalyRules_shouldNotTrigger_whenNotAnomaly() {
        testAnomaly.setIsAnomaly(false);
        when(alertRuleRepository.findByTypeAndEnabledTrue(RuleType.ANOMALY_DETECTION))
                .thenReturn(List.of(testRule));

        List<AlertRule> triggeredRules = alertRuleEngine.evaluateAnomalyRules(testAnomaly);

        assertThat(triggeredRules).isEmpty();
        verify(alertService, never()).createAlertFromAnomaly(any(), any());
    }

    @Test
    void evaluateAnomalyRules_shouldFilterByService_whenServicesConfigured() {
        testRule.setServices("other-service,another-service");
        when(alertRuleRepository.findByTypeAndEnabledTrue(RuleType.ANOMALY_DETECTION))
                .thenReturn(List.of(testRule));

        List<AlertRule> triggeredRules = alertRuleEngine.evaluateAnomalyRules(testAnomaly);

        assertThat(triggeredRules).isEmpty();
    }

    @Test
    void evaluateAnomalyRules_shouldFilterByLogLevel_whenLogLevelsConfigured() {
        testRule.setLogLevels("WARN,INFO");
        when(alertRuleRepository.findByTypeAndEnabledTrue(RuleType.ANOMALY_DETECTION))
                .thenReturn(List.of(testRule));

        List<AlertRule> triggeredRules = alertRuleEngine.evaluateAnomalyRules(testAnomaly);

        assertThat(triggeredRules).isEmpty();
    }

    @Test
    void evaluateRule_shouldReturnTrue_whenRuleMatches() {
        when(alertRuleRepository.findById(1L)).thenReturn(Optional.of(testRule));

        boolean result = alertRuleEngine.evaluateRule(1L, testAnomaly);

        assertThat(result).isTrue();
    }

    @Test
    void evaluateRule_shouldReturnFalse_whenRuleDisabled() {
        testRule.setEnabled(false);
        when(alertRuleRepository.findById(1L)).thenReturn(Optional.of(testRule));

        boolean result = alertRuleEngine.evaluateRule(1L, testAnomaly);

        assertThat(result).isFalse();
    }

    @Test
    void evaluateRule_shouldReturnFalse_whenWrongRuleType() {
        testRule.setType(RuleType.THRESHOLD);
        when(alertRuleRepository.findById(1L)).thenReturn(Optional.of(testRule));

        boolean result = alertRuleEngine.evaluateRule(1L, testAnomaly);

        assertThat(result).isFalse();
    }

    @Test
    void evaluateRule_shouldThrowException_whenRuleNotFound() {
        when(alertRuleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alertRuleEngine.evaluateRule(999L, testAnomaly))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rule not found");
    }

    @Test
    void getMatchingRules_shouldReturnMatchingRules() {
        when(alertRuleRepository.findByTypeAndEnabledTrue(RuleType.ANOMALY_DETECTION))
                .thenReturn(List.of(testRule));

        List<AlertRule> matchingRules = alertRuleEngine.getMatchingRules(testAnomaly);

        assertThat(matchingRules).hasSize(1);
        verify(alertService, never()).createAlertFromAnomaly(any(), any());
    }

    @Test
    void testRule_shouldReturnEvaluationResult() {
        AlertRuleEngine.RuleEvaluationResult result = alertRuleEngine.testRule(testRule, testAnomaly);

        assertThat(result.isTriggered()).isTrue();
        assertThat(result.getRuleName()).isEqualTo("Test Rule");
        assertThat(result.getAnomalyId()).isEqualTo("log-123");
        assertThat(result.getEvaluationDetails()).isNotEmpty();
    }

    @Test
    void testRule_shouldIncludeConfidenceCheck_whenThresholdSet() {
        AlertRuleEngine.RuleEvaluationResult result = alertRuleEngine.testRule(testRule, testAnomaly);

        assertThat(result.getEvaluationDetails())
                .anyMatch(detail -> detail.contains("Confidence check"));
    }
}
