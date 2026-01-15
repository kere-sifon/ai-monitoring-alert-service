package com.ibm.aimonitoring.alert.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.aimonitoring.alert.dto.AcknowledgeAlertRequest;
import com.ibm.aimonitoring.alert.dto.ResolveAlertRequest;
import com.ibm.aimonitoring.alert.model.Alert;
import com.ibm.aimonitoring.alert.model.AlertRule;
import com.ibm.aimonitoring.alert.model.AlertStatus;
import com.ibm.aimonitoring.alert.model.Severity;
import com.ibm.aimonitoring.alert.model.RuleType;
import com.ibm.aimonitoring.alert.service.AlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for AlertController
 */
@WebMvcTest(AlertController.class)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
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
    void shouldGetAllAlertsWithPagination() throws Exception {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Alert> alertPage = new PageImpl<>(Arrays.asList(testAlert), pageable, 1);
        when(alertService.getAllAlerts(any(Pageable.class))).thenReturn(alertPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/alerts")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sort", "createdAt", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Test Alert"))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));

        verify(alertService, times(1)).getAllAlerts(any(Pageable.class));
    }

    @Test
    void shouldGetAlertById() throws Exception {
        // Arrange
        when(alertService.getAlert(1L)).thenReturn(testAlert);

        // Act & Assert
        mockMvc.perform(get("/api/v1/alerts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Alert"))
                .andExpect(jsonPath("$.alertRuleName").value("Test Rule"));

        verify(alertService, times(1)).getAlert(1L);
    }

    @Test
    void shouldGetAlertsByStatus() throws Exception {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Alert> alertPage = new PageImpl<>(Arrays.asList(testAlert), pageable, 1);
        when(alertService.getAlertsByStatus(eq(AlertStatus.OPEN), any(Pageable.class)))
                .thenReturn(alertPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/alerts/status/OPEN")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));

        verify(alertService, times(1)).getAlertsByStatus(eq(AlertStatus.OPEN), any(Pageable.class));
    }

    @Test
    void shouldGetOpenAlerts() throws Exception {
        // Arrange
        List<Alert> alerts = Arrays.asList(testAlert);
        when(alertService.getOpenAlerts()).thenReturn(alerts);

        // Act & Assert
        mockMvc.perform(get("/api/v1/alerts/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("OPEN"));

        verify(alertService, times(1)).getOpenAlerts();
    }

    @Test
    void shouldGetRecentAlerts() throws Exception {
        // Arrange
        List<Alert> alerts = Arrays.asList(testAlert);
        when(alertService.getRecentAlerts(eq(60), any(Pageable.class))).thenReturn(alerts);

        // Act & Assert
        mockMvc.perform(get("/api/v1/alerts/recent")
                        .param("minutes", "60")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(alertService, times(1)).getRecentAlerts(eq(60), any(Pageable.class));
    }

    @Test
    void shouldGetAlertsByService() throws Exception {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Alert> alertPage = new PageImpl<>(Arrays.asList(testAlert), pageable, 1);
        when(alertService.getAlertsByService(eq("test-service"), any(Pageable.class)))
                .thenReturn(alertPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/alerts/service/test-service")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].service").value("test-service"));

        verify(alertService, times(1)).getAlertsByService(eq("test-service"), any(Pageable.class));
    }

    @Test
    void shouldAcknowledgeAlert() throws Exception {
        // Arrange
        Alert acknowledgedAlert = Alert.builder()
                .id(1L)
                .alertRule(testAlertRule)
                .status(AlertStatus.ACKNOWLEDGED)
                .severity(Severity.HIGH)
                .title("Test Alert")
                .description("Test alert description")
                .service("test-service")
                .acknowledgedBy("admin")
                .acknowledgedAt(LocalDateTime.now())
                .build();

        AcknowledgeAlertRequest request = new AcknowledgeAlertRequest();
        request.setAcknowledgedBy("admin");

        when(alertService.acknowledgeAlert(1L, "admin")).thenReturn(acknowledgedAlert);

        // Act & Assert
        mockMvc.perform(post("/api/v1/alerts/1/acknowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.acknowledgedBy").value("admin"));

        verify(alertService, times(1)).acknowledgeAlert(1L, "admin");
    }

    @Test
    void shouldResolveAlert() throws Exception {
        // Arrange
        Alert resolvedAlert = Alert.builder()
                .id(1L)
                .alertRule(testAlertRule)
                .status(AlertStatus.RESOLVED)
                .severity(Severity.HIGH)
                .title("Test Alert")
                .description("Test alert description")
                .service("test-service")
                .resolvedBy("admin")
                .resolvedAt(LocalDateTime.now())
                .resolutionNotes("Fixed the issue")
                .build();

        ResolveAlertRequest request = new ResolveAlertRequest();
        request.setResolvedBy("admin");
        request.setNotes("Fixed the issue");

        when(alertService.resolveAlert(1L, "admin", "Fixed the issue")).thenReturn(resolvedAlert);

        // Act & Assert
        mockMvc.perform(post("/api/v1/alerts/1/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedBy").value("admin"))
                .andExpect(jsonPath("$.resolutionNotes").value("Fixed the issue"));

        verify(alertService, times(1)).resolveAlert(1L, "admin", "Fixed the issue");
    }

    @Test
    void shouldMarkAsFalsePositive() throws Exception {
        // Arrange
        Alert falsePositiveAlert = Alert.builder()
                .id(1L)
                .alertRule(testAlertRule)
                .status(AlertStatus.FALSE_POSITIVE)
                .severity(Severity.HIGH)
                .title("Test Alert")
                .description("Test alert description")
                .service("test-service")
                .build();

        when(alertService.markAsFalsePositive(1L, "admin")).thenReturn(falsePositiveAlert);

        // Act & Assert
        mockMvc.perform(post("/api/v1/alerts/1/false-positive")
                        .param("markedBy", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FALSE_POSITIVE"));

        verify(alertService, times(1)).markAsFalsePositive(1L, "admin");
    }

    @Test
    void shouldRetryNotifications() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/alerts/1/retry-notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Notification retry initiated"))
                .andExpect(jsonPath("$.alertId").value("1"));

        verify(alertService, times(1)).retryNotifications(1L);
    }

    @Test
    void shouldGetStatistics() throws Exception {
        // Arrange
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", 100L);
        stats.put("open", 30L);
        stats.put("acknowledged", 20L);
        stats.put("resolved", 45L);
        stats.put("false_positive", 5L);

        when(alertService.getAlertStatistics()).thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/v1/alerts/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(100))
                .andExpect(jsonPath("$.open").value(30))
                .andExpect(jsonPath("$.acknowledged").value(20))
                .andExpect(jsonPath("$.resolved").value(45));

        verify(alertService, times(1)).getAlertStatistics();
    }

    @Test
    void shouldGetAlertsBySeverity() throws Exception {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Alert> alertPage = new PageImpl<>(Arrays.asList(testAlert), pageable, 1);
        when(alertService.getAllAlerts(any(Pageable.class))).thenReturn(alertPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/alerts/severity/HIGH")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        verify(alertService, times(1)).getAllAlerts(any(Pageable.class));
    }
}

// Made with Bob