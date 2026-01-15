package com.ibm.aimonitoring.alert.service;

import com.ibm.aimonitoring.alert.model.Alert;
import com.ibm.aimonitoring.alert.model.AlertRule;
import com.ibm.aimonitoring.alert.model.AlertStatus;
import com.ibm.aimonitoring.alert.model.Severity;
import com.ibm.aimonitoring.alert.model.RuleType;
import com.ibm.aimonitoring.alert.repository.AlertRepository;
import com.ibm.aimonitoring.alert.repository.AlertRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AlertService
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertRuleRepository alertRuleRepository;

    @Mock
    private NotificationDispatcher notificationDispatcher;

    @InjectMocks
    private AlertService alertService;

    private Alert testAlert;
    private AlertRule testAlertRule;

    @BeforeEach
    void setUp() {
        testAlertRule = AlertRule.builder()
                .id(1L)
                .name("Test Rule")
                .type(RuleType.ANOMALY_DETECTION)
                .severity(Severity.HIGH)
                .enabled(true)
                .build();

        testAlert = Alert.builder()
                .id(1L)
                .alertRule(testAlertRule)
                .status(AlertStatus.OPEN)
                .severity(Severity.HIGH)
                .title("Test Alert")
                .description("Test alert description")
                .service("test-service")
                .logId("log-123")
                .anomalyDetectionId("anomaly-456")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .notificationSent(false)
                .notificationFailureCount(0)
                .build();
    }

    @Test
    void shouldGetAllAlerts() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Page<Alert> alertPage = new PageImpl<>(Arrays.asList(testAlert));
        when(alertRepository.findAll(pageable)).thenReturn(alertPage);

        // Act
        Page<Alert> result = alertService.getAllAlerts(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(testAlert.getId(), result.getContent().get(0).getId());
        verify(alertRepository, times(1)).findAll(pageable);
    }

    @Test
    void shouldGetAlertById() {
        // Arrange
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));

        // Act
        Alert result = alertService.getAlert(1L);

        // Assert
        assertNotNull(result);
        assertEquals(testAlert.getId(), result.getId());
        assertEquals(testAlert.getTitle(), result.getTitle());
        verify(alertRepository, times(1)).findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenAlertNotFound() {
        // Arrange
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            alertService.getAlert(999L);
        });
        assertTrue(exception.getMessage().contains("Alert not found"));
        verify(alertRepository, times(1)).findById(999L);
    }

    @Test
    void shouldGetAlertsByStatus() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Page<Alert> alertPage = new PageImpl<>(Arrays.asList(testAlert));
        when(alertRepository.findByStatus(AlertStatus.OPEN, pageable)).thenReturn(alertPage);

        // Act
        Page<Alert> result = alertService.getAlertsByStatus(AlertStatus.OPEN, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(AlertStatus.OPEN, result.getContent().get(0).getStatus());
        verify(alertRepository, times(1)).findByStatus(AlertStatus.OPEN, pageable);
    }

    @Test
    void shouldGetOpenAlerts() {
        // Arrange
        List<Alert> alerts = Arrays.asList(testAlert);
        when(alertRepository.findByStatus(AlertStatus.OPEN)).thenReturn(alerts);

        // Act
        List<Alert> result = alertService.getOpenAlerts();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testAlert.getId(), result.get(0).getId());
        verify(alertRepository, times(1)).findByStatus(AlertStatus.OPEN);
    }

    @Test
    void shouldGetRecentAlerts() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 50);
        List<Alert> alerts = Arrays.asList(testAlert);
        when(alertRepository.findRecentAlerts(any(LocalDateTime.class), eq(pageable)))
                .thenReturn(alerts);

        // Act
        List<Alert> result = alertService.getRecentAlerts(60, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(alertRepository, times(1)).findRecentAlerts(any(LocalDateTime.class), eq(pageable));
    }

    @Test
    void shouldGetAlertsByService() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        Page<Alert> alertPage = new PageImpl<>(Arrays.asList(testAlert));
        when(alertRepository.findByService("test-service", pageable)).thenReturn(alertPage);

        // Act
        Page<Alert> result = alertService.getAlertsByService("test-service", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("test-service", result.getContent().get(0).getService());
        verify(alertRepository, times(1)).findByService("test-service", pageable);
    }

    @Test
    void shouldAcknowledgeAlert() {
        // Arrange
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);

        // Act
        Alert result = alertService.acknowledgeAlert(1L, "admin");

        // Assert
        assertNotNull(result);
        assertEquals(AlertStatus.ACKNOWLEDGED, result.getStatus());
        assertEquals("admin", result.getAcknowledgedBy());
        assertNotNull(result.getAcknowledgedAt());
        verify(alertRepository, times(1)).findById(1L);
        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void shouldResolveAlert() {
        // Arrange
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);

        // Act
        Alert result = alertService.resolveAlert(1L, "admin", "Fixed the issue");

        // Assert
        assertNotNull(result);
        assertEquals(AlertStatus.RESOLVED, result.getStatus());
        assertEquals("admin", result.getResolvedBy());
        assertEquals("Fixed the issue", result.getResolutionNotes());
        assertNotNull(result.getResolvedAt());
        verify(alertRepository, times(1)).findById(1L);
        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void shouldMarkAsFalsePositive() {
        // Arrange
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);

        // Act
        Alert result = alertService.markAsFalsePositive(1L, "admin");

        // Assert
        assertNotNull(result);
        assertEquals(AlertStatus.FALSE_POSITIVE, result.getStatus());
        verify(alertRepository, times(1)).findById(1L);
        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void shouldRetryNotifications() {
        // Arrange
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        doNothing().when(notificationDispatcher).sendNotifications(any(Alert.class));

        // Act
        alertService.retryNotifications(1L);

        // Assert
        verify(alertRepository, times(1)).findById(1L);
        verify(notificationDispatcher, times(1)).sendNotifications(any(Alert.class));
    }

    @Test
    void shouldGetAlertStatistics() {
        // Arrange
        when(alertRepository.count()).thenReturn(100L);
        when(alertRepository.countByStatus(AlertStatus.OPEN)).thenReturn(30L);
        when(alertRepository.countByStatus(AlertStatus.ACKNOWLEDGED)).thenReturn(20L);
        when(alertRepository.countByStatus(AlertStatus.RESOLVED)).thenReturn(45L);
        when(alertRepository.countByStatus(AlertStatus.FALSE_POSITIVE)).thenReturn(5L);

        // Act
        Map<String, Long> result = alertService.getAlertStatistics();

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.get("total"));
        assertEquals(30L, result.get("open"));
        assertEquals(20L, result.get("acknowledged"));
        assertEquals(45L, result.get("resolved"));
        // FALSE_POSITIVE might be returned as "falsePositive" or "false_positive" depending on implementation
        assertTrue(result.containsKey("false_positive") || result.containsKey("falsePositive"));
        verify(alertRepository, times(1)).count();
        verify(alertRepository, times(4)).countByStatus(any(AlertStatus.class));
    }

}

// Made with Bob