package com.ibm.aimonitoring.alert.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertRuleTest {

    private AlertRule alertRule;

    @BeforeEach
    void setUp() {
        alertRule = AlertRule.builder()
                .id(1L)
                .name("Test Rule")
                .type(RuleType.ANOMALY_DETECTION)
                .severity(Severity.HIGH)
                .enabled(true)
                .triggerCount(0L)
                .build();
    }

    @Test
    void incrementTriggerCount_shouldIncrementAndSetTimestamp() {
        alertRule.incrementTriggerCount();

        assertThat(alertRule.getTriggerCount()).isEqualTo(1L);
        assertThat(alertRule.getLastTriggeredAt()).isNotNull();

        alertRule.incrementTriggerCount();
        assertThat(alertRule.getTriggerCount()).isEqualTo(2L);
    }

    @Test
    void builder_shouldSetDefaultTriggerCount() {
        AlertRule newRule = AlertRule.builder()
                .name("New Rule")
                .build();

        assertThat(newRule.getTriggerCount()).isEqualTo(0L);
    }
}
