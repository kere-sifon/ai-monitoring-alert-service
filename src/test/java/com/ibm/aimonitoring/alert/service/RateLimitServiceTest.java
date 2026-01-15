package com.ibm.aimonitoring.alert.service;

import com.ibm.aimonitoring.alert.model.AlertRule;
import com.ibm.aimonitoring.alert.model.RuleType;
import com.ibm.aimonitoring.alert.model.Severity;
import com.ibm.aimonitoring.alert.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {

    @Mock
    private AlertRepository alertRepository;

    private RateLimitService rateLimitService;
    private AlertRule testRule;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(alertRepository);
        ReflectionTestUtils.setField(rateLimitService, "enabled", true);
        ReflectionTestUtils.setField(rateLimitService, "maxAlertsPerRule", 10);
        ReflectionTestUtils.setField(rateLimitService, "timeWindowMinutes", 60);
        ReflectionTestUtils.setField(rateLimitService, "cooldownMinutes", 15);

        testRule = AlertRule.builder()
                .id(1L)
                .name("Test Rule")
                .type(RuleType.ANOMALY_DETECTION)
                .severity(Severity.HIGH)
                .enabled(true)
                .build();
    }

    @Test
    void isAlertAllowed_shouldReturnTrue_whenUnderLimit() {
        when(alertRepository.countByRuleIdSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(5L);

        boolean allowed = rateLimitService.isAlertAllowed(testRule);

        assertThat(allowed).isTrue();
    }

    @Test
    void isAlertAllowed_shouldReturnFalse_whenOverLimit() {
        when(alertRepository.countByRuleIdSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(15L);

        boolean allowed = rateLimitService.isAlertAllowed(testRule);

        assertThat(allowed).isFalse();
    }

    @Test
    void isAlertAllowed_shouldReturnTrue_whenDisabled() {
        ReflectionTestUtils.setField(rateLimitService, "enabled", false);

        boolean allowed = rateLimitService.isAlertAllowed(testRule);

        assertThat(allowed).isTrue();
    }

    @Test
    void isAlertAllowed_shouldReturnFalse_whenInCooldown() {
        // First trigger cooldown by exceeding limit
        when(alertRepository.countByRuleIdSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(15L);
        rateLimitService.isAlertAllowed(testRule);

        // Reset mock to return low count
        when(alertRepository.countByRuleIdSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(0L);

        // Should still be blocked due to cooldown
        boolean allowed = rateLimitService.isAlertAllowed(testRule);

        assertThat(allowed).isFalse();
    }

    @Test
    void clearCooldown_shouldAllowAlerts_afterCooldownCleared() {
        // Trigger cooldown
        when(alertRepository.countByRuleIdSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(15L);
        rateLimitService.isAlertAllowed(testRule);

        // Clear cooldown
        rateLimitService.clearCooldown(1L);

        // Reset mock
        when(alertRepository.countByRuleIdSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(0L);

        boolean allowed = rateLimitService.isAlertAllowed(testRule);

        assertThat(allowed).isTrue();
    }

    @Test
    void getCooldownStatus_shouldReturnNotInCooldown_whenNoCooldown() {
        Map<String, Object> status = rateLimitService.getCooldownStatus(1L);

        assertThat(status.get("inCooldown")).isEqualTo(false);
    }

    @Test
    void getCooldownStatus_shouldReturnInCooldown_whenInCooldown() {
        // Trigger cooldown
        when(alertRepository.countByRuleIdSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(15L);
        rateLimitService.isAlertAllowed(testRule);

        Map<String, Object> status = rateLimitService.getCooldownStatus(1L);

        assertThat(status.get("inCooldown")).isEqualTo(true);
        assertThat(status.get("cooldownUntil")).isNotEqualTo("");
    }

    @Test
    void getAlertCount_shouldReturnCount() {
        when(alertRepository.countByRuleIdSince(eq(1L), any(LocalDateTime.class)))
                .thenReturn(7L);

        long count = rateLimitService.getAlertCount(1L);

        assertThat(count).isEqualTo(7L);
    }

    @Test
    void isEnabled_shouldReturnEnabledStatus() {
        assertThat(rateLimitService.isEnabled()).isTrue();

        ReflectionTestUtils.setField(rateLimitService, "enabled", false);
        assertThat(rateLimitService.isEnabled()).isFalse();
    }

    @Test
    void getConfiguration_shouldReturnAllSettings() {
        Map<String, Object> config = rateLimitService.getConfiguration();

        assertThat(config).containsEntry("enabled", true);
        assertThat(config).containsEntry("maxAlertsPerRule", 10);
        assertThat(config).containsEntry("timeWindowMinutes", 60);
        assertThat(config).containsEntry("cooldownMinutes", 15);
    }
}
