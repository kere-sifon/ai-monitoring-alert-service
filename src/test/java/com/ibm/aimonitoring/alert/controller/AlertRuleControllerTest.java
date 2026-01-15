package com.ibm.aimonitoring.alert.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.aimonitoring.alert.dto.CreateAlertRuleRequest;
import com.ibm.aimonitoring.alert.dto.UpdateAlertRuleRequest;
import com.ibm.aimonitoring.alert.model.AlertRule;
import com.ibm.aimonitoring.alert.model.RuleType;
import com.ibm.aimonitoring.alert.model.Severity;
import com.ibm.aimonitoring.alert.repository.AlertRuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlertRuleController.class)
class AlertRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AlertRuleRepository alertRuleRepository;

    private AlertRule testRule;

    @BeforeEach
    void setUp() {
        testRule = AlertRule.builder()
                .id(1L)
                .name("Test Rule")
                .description("Test Description")
                .type(RuleType.ANOMALY_DETECTION)
                .severity(Severity.HIGH)
                .enabled(true)
                .anomalyThreshold(0.7)
                .cooldownMinutes(15)
                .notifyOnRecovery(false)
                .triggerCount(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createAlertRule_shouldReturnCreated() throws Exception {
        CreateAlertRuleRequest request = new CreateAlertRuleRequest();
        request.setName("New Rule");
        request.setType(RuleType.ANOMALY_DETECTION);
        request.setSeverity(Severity.HIGH);
        request.setEnabled(true);

        when(alertRuleRepository.save(any(AlertRule.class))).thenReturn(testRule);

        mockMvc.perform(post("/api/v1/alert-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Rule"));
    }

    @Test
    void getAlertRuleById_shouldReturnRule_whenFound() throws Exception {
        when(alertRuleRepository.findById(1L)).thenReturn(Optional.of(testRule));

        mockMvc.perform(get("/api/v1/alert-rules/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Rule"));
    }

    @Test
    void getAlertRuleById_shouldReturn404_whenNotFound() throws Exception {
        when(alertRuleRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/alert-rules/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEnabledAlertRules_shouldReturnEnabledRules() throws Exception {
        when(alertRuleRepository.findByEnabledTrue()).thenReturn(List.of(testRule));

        mockMvc.perform(get("/api/v1/alert-rules/enabled"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enabled").value(true));
    }

    @Test
    void getAlertRulesByType_shouldReturnRulesOfType() throws Exception {
        when(alertRuleRepository.findByType(RuleType.ANOMALY_DETECTION))
                .thenReturn(List.of(testRule));

        mockMvc.perform(get("/api/v1/alert-rules/type/ANOMALY_DETECTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("ANOMALY_DETECTION"));
    }

    @Test
    void getAlertRulesBySeverity_shouldReturnRulesOfSeverity() throws Exception {
        when(alertRuleRepository.findBySeverity(Severity.HIGH)).thenReturn(List.of(testRule));

        mockMvc.perform(get("/api/v1/alert-rules/severity/HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].severity").value("HIGH"));
    }

    @Test
    void updateAlertRule_shouldReturnUpdatedRule() throws Exception {
        UpdateAlertRuleRequest request = new UpdateAlertRuleRequest();
        request.setName("Updated Rule");

        when(alertRuleRepository.findById(1L)).thenReturn(Optional.of(testRule));
        when(alertRuleRepository.save(any(AlertRule.class))).thenReturn(testRule);

        mockMvc.perform(put("/api/v1/alert-rules/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateAlertRule_shouldReturn404_whenNotFound() throws Exception {
        UpdateAlertRuleRequest request = new UpdateAlertRuleRequest();
        request.setName("Updated Rule");

        when(alertRuleRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/v1/alert-rules/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void enableAlertRule_shouldEnableRule() throws Exception {
        testRule.setEnabled(false);
        when(alertRuleRepository.findById(1L)).thenReturn(Optional.of(testRule));
        when(alertRuleRepository.save(any(AlertRule.class))).thenReturn(testRule);

        mockMvc.perform(post("/api/v1/alert-rules/1/enable"))
                .andExpect(status().isOk());

        verify(alertRuleRepository).save(argThat(AlertRule::getEnabled));
    }

    @Test
    void disableAlertRule_shouldDisableRule() throws Exception {
        when(alertRuleRepository.findById(1L)).thenReturn(Optional.of(testRule));
        when(alertRuleRepository.save(any(AlertRule.class))).thenReturn(testRule);

        mockMvc.perform(post("/api/v1/alert-rules/1/disable"))
                .andExpect(status().isOk());

        verify(alertRuleRepository).save(argThat(rule -> !rule.getEnabled()));
    }

    @Test
    void deleteAlertRule_shouldReturn204() throws Exception {
        when(alertRuleRepository.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/alert-rules/1"))
                .andExpect(status().isNoContent());

        verify(alertRuleRepository).deleteById(1L);
    }

    @Test
    void deleteAlertRule_shouldReturn404_whenNotFound() throws Exception {
        when(alertRuleRepository.existsById(999L)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/alert-rules/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testAlertRule_shouldReturnTestResult() throws Exception {
        when(alertRuleRepository.findById(1L)).thenReturn(Optional.of(testRule));

        mockMvc.perform(post("/api/v1/alert-rules/1/test"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Test Rule")));
    }
}
