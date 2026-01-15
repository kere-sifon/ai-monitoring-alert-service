package com.ibm.aimonitoring.alert.service.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.aimonitoring.alert.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SlackNotificationServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    private ObjectMapper objectMapper = new ObjectMapper();

    private SlackNotificationService service;

    private Alert testAlert;
    private NotificationChannel testChannel;

    @BeforeEach
    void setUp() {
        service = new SlackNotificationService(webClientBuilder, objectMapper);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "timeout", 5000);

        AlertRule alertRule = AlertRule.builder()
                .id(1L)
                .name("Test Rule")
                .type(RuleType.ANOMALY_DETECTION)
                .severity(Severity.HIGH)
                .build();

        testAlert = Alert.builder()
                .id(1L)
                .alertRule(alertRule)
                .status(AlertStatus.OPEN)
                .severity(Severity.CRITICAL)
                .title("Test Alert")
                .description("Test description")
                .service("test-service")
                .createdAt(LocalDateTime.now())
                .build();

        testChannel = NotificationChannel.builder()
                .id(1L)
                .name("Test Slack")
                .type(ChannelType.SLACK)
                .slackChannel("https://hooks.slack.com/services/xxx")
                .enabled(true)
                .build();
    }

    @Test
    void isEnabled_shouldReturnTrue() {
        assertThat(service.isEnabled()).isTrue();
    }

    @Test
    void isEnabled_shouldReturnFalseWhenDisabled() {
        ReflectionTestUtils.setField(service, "enabled", false);
        assertThat(service.isEnabled()).isFalse();
    }

    @Test
    void sendNotification_shouldThrowWhenNoSlackChannel() {
        testChannel.setSlackChannel(null);

        assertThatThrownBy(() -> service.sendNotification(testAlert, testChannel))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("No Slack webhook URL configured");
    }

    @Test
    void sendNotification_shouldSkipWhenDisabled() throws NotificationException {
        ReflectionTestUtils.setField(service, "enabled", false);
        
        service.sendNotification(testAlert, testChannel);
        
        assertThat(service.isEnabled()).isFalse();
    }

    @Test
    void testConnection_shouldReturnFalseWhenNoChannel() {
        testChannel.setSlackChannel(null);

        boolean result = service.testConnection(testChannel);

        assertThat(result).isFalse();
    }

    @Test
    void testConnection_shouldReturnFalseWhenEmptyChannel() {
        testChannel.setSlackChannel("");

        boolean result = service.testConnection(testChannel);

        assertThat(result).isFalse();
    }

    @Test
    void sendNotification_shouldThrowWhenEmptySlackChannel() {
        testChannel.setSlackChannel("");

        assertThatThrownBy(() -> service.sendNotification(testAlert, testChannel))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("No Slack webhook URL configured");
    }
}
