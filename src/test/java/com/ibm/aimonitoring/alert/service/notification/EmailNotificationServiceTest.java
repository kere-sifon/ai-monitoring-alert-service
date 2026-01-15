package com.ibm.aimonitoring.alert.service.notification;

import com.ibm.aimonitoring.alert.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;

    private EmailNotificationService service;

    private Alert testAlert;
    private NotificationChannel testChannel;

    @BeforeEach
    void setUp() {
        service = new EmailNotificationService(mailSender);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "fromEmail", "alerts@test.com");
        ReflectionTestUtils.setField(service, "fromName", "Test System");

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
                .severity(Severity.HIGH)
                .title("Test Alert")
                .description("Test description")
                .service("test-service")
                .createdAt(LocalDateTime.now())
                .build();

        testChannel = NotificationChannel.builder()
                .id(1L)
                .name("Test Email")
                .type(ChannelType.EMAIL)
                .recipients("test@example.com")
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
    void sendNotification_shouldThrowWhenNoRecipients() {
        testChannel.setRecipients(null);

        assertThatThrownBy(() -> service.sendNotification(testAlert, testChannel))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("No recipients configured");
    }

    @Test
    void sendNotification_shouldSkipWhenDisabled() throws NotificationException {
        ReflectionTestUtils.setField(service, "enabled", false);
        
        service.sendNotification(testAlert, testChannel);
        // Should complete without exception
    }

    @Test
    void testConnection_shouldReturnTrueWhenMailSenderWorks() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        boolean result = service.testConnection(testChannel);

        assertThat(result).isTrue();
    }

    @Test
    void testConnection_shouldReturnFalseOnException() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Connection failed"));

        boolean result = service.testConnection(testChannel);

        assertThat(result).isFalse();
    }
}
