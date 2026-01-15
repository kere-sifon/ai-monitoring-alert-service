package com.ibm.aimonitoring.alert.service;

import com.ibm.aimonitoring.alert.model.*;
import com.ibm.aimonitoring.alert.repository.NotificationChannelRepository;
import com.ibm.aimonitoring.alert.service.notification.EmailNotificationService;
import com.ibm.aimonitoring.alert.service.notification.NotificationException;
import com.ibm.aimonitoring.alert.service.notification.SlackNotificationService;
import com.ibm.aimonitoring.alert.service.notification.WebhookNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock
    private NotificationChannelRepository channelRepository;

    @Mock
    private EmailNotificationService emailService;

    @Mock
    private SlackNotificationService slackService;

    @Mock
    private WebhookNotificationService webhookService;

    private NotificationDispatcher notificationDispatcher;
    private Alert testAlert;
    private AlertRule testRule;
    private NotificationChannel emailChannel;
    private NotificationChannel slackChannel;

    @BeforeEach
    void setUp() {
        notificationDispatcher = new NotificationDispatcher(
                channelRepository, emailService, slackService, webhookService);

        testRule = AlertRule.builder()
                .id(1L)
                .name("Test Rule")
                .type(RuleType.ANOMALY_DETECTION)
                .severity(Severity.HIGH)
                .enabled(true)
                .build();

        testAlert = Alert.builder()
                .id(1L)
                .alertRule(testRule)
                .status(AlertStatus.OPEN)
                .severity(Severity.HIGH)
                .title("Test Alert")
                .build();

        emailChannel = NotificationChannel.builder()
                .id(1L)
                .name("Email Channel")
                .type(ChannelType.EMAIL)
                .enabled(true)
                .alertRule(testRule)
                .recipients("test@example.com")
                .successCount(0L)
                .failureCount(0L)
                .build();

        slackChannel = NotificationChannel.builder()
                .id(2L)
                .name("Slack Channel")
                .type(ChannelType.SLACK)
                .enabled(true)
                .alertRule(testRule)
                .slackChannel("https://hooks.slack.com/test")
                .successCount(0L)
                .failureCount(0L)
                .build();
    }

    @Test
    void sendNotifications_shouldSendToAllEnabledChannels() throws NotificationException {
        when(channelRepository.findByAlertRuleIdAndEnabledTrue(1L))
                .thenReturn(List.of(emailChannel, slackChannel));
        when(emailService.isEnabled()).thenReturn(true);
        when(slackService.isEnabled()).thenReturn(true);

        notificationDispatcher.sendNotifications(testAlert);

        verify(emailService).sendNotification(testAlert, emailChannel);
        verify(slackService).sendNotification(testAlert, slackChannel);
        verify(channelRepository).saveAll(any());
    }

    @Test
    void sendNotifications_shouldHandleNoChannels() throws NotificationException {
        when(channelRepository.findByAlertRuleIdAndEnabledTrue(1L))
                .thenReturn(List.of());

        notificationDispatcher.sendNotifications(testAlert);

        verify(emailService, never()).sendNotification(any(), any());
        verify(slackService, never()).sendNotification(any(), any());
    }

    @Test
    void sendNotifications_shouldContinueOnFailure() throws NotificationException {
        when(channelRepository.findByAlertRuleIdAndEnabledTrue(1L))
                .thenReturn(List.of(emailChannel, slackChannel));
        when(emailService.isEnabled()).thenReturn(true);
        when(slackService.isEnabled()).thenReturn(true);
        doThrow(new NotificationException("Email failed")).when(emailService)
                .sendNotification(any(), any());

        notificationDispatcher.sendNotifications(testAlert);

        verify(slackService).sendNotification(testAlert, slackChannel);
        verify(channelRepository).saveAll(any());
    }

    @Test
    void sendNotification_shouldThrowException_whenServiceFails() throws NotificationException {
        // Test that exceptions from services are propagated
        when(emailService.isEnabled()).thenReturn(true);
        doThrow(new NotificationException("Test failure")).when(emailService).sendNotification(any(), any());

        assertThatThrownBy(() -> notificationDispatcher.sendNotification(testAlert, emailChannel))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("Test failure");
    }

    @Test
    void sendNotification_shouldSkip_whenServiceDisabled() throws NotificationException {
        when(emailService.isEnabled()).thenReturn(false);

        notificationDispatcher.sendNotification(testAlert, emailChannel);

        verify(emailService, never()).sendNotification(any(), any());
    }

    @Test
    void testChannel_shouldReturnTrue_whenTestSucceeds() {
        when(emailService.isEnabled()).thenReturn(true);
        when(emailService.testConnection(emailChannel)).thenReturn(true);

        boolean result = notificationDispatcher.testChannel(emailChannel);

        assertThat(result).isTrue();
    }

    @Test
    void testChannel_shouldReturnFalse_whenServiceDisabled() {
        when(emailService.isEnabled()).thenReturn(false);

        boolean result = notificationDispatcher.testChannel(emailChannel);

        assertThat(result).isFalse();
    }

    @Test
    void testChannel_shouldReturnFalse_whenTestFails() {
        when(emailService.isEnabled()).thenReturn(true);
        when(emailService.testConnection(emailChannel)).thenReturn(false);

        boolean result = notificationDispatcher.testChannel(emailChannel);

        assertThat(result).isFalse();
    }

    @Test
    void getChannelStatistics_shouldReturnStats() {
        emailChannel.setSuccessCount(10L);
        emailChannel.setFailureCount(2L);
        when(channelRepository.findById(1L)).thenReturn(Optional.of(emailChannel));

        Map<String, Long> stats = notificationDispatcher.getChannelStatistics(1L);

        assertThat(stats)
                .containsEntry("successCount", 10L)
                .containsEntry("failureCount", 2L)
                .containsEntry("totalCount", 12L);
    }

    @Test
    void getChannelStatistics_shouldThrowException_whenChannelNotFound() {
        when(channelRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationDispatcher.getChannelStatistics(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Channel not found");
    }

}
