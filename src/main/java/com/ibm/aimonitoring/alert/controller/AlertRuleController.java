package com.ibm.aimonitoring.alert.controller;

import com.ibm.aimonitoring.alert.dto.AlertRuleDTO;
import com.ibm.aimonitoring.alert.dto.CreateAlertRuleRequest;
import com.ibm.aimonitoring.alert.dto.UpdateAlertRuleRequest;
import com.ibm.aimonitoring.alert.model.AlertRule;
import com.ibm.aimonitoring.alert.model.RuleType;
import com.ibm.aimonitoring.alert.model.Severity;
import com.ibm.aimonitoring.alert.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for managing alert rules.
 * Provides CRUD operations and rule activation/deactivation.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/alert-rules")
@RequiredArgsConstructor
public class AlertRuleController {

    private final AlertRuleRepository alertRuleRepository;

    /**
     * Create a new alert rule
     */
    @PostMapping
    public ResponseEntity<AlertRuleDTO> createAlertRule(@Valid @RequestBody CreateAlertRuleRequest request) {
        log.info("Creating new alert rule: {}", request.getName());
        
        AlertRule alertRule = AlertRule.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .severity(request.getSeverity())
                .enabled(request.isEnabled())
                .anomalyThreshold(request.getAnomalyThreshold())
                .conditions(request.getConditions())
                .serviceName(request.getServiceName())
                .logLevel(request.getLogLevel())
                .timeWindowMinutes(request.getTimeWindowMinutes())
                .threshold(request.getThreshold())
                .cooldownMinutes(request.getCooldownMinutes())
                .notifyOnRecovery(request.isNotifyOnRecovery())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        AlertRule saved = alertRuleRepository.save(alertRule);
        log.info("Alert rule created successfully with ID: {}", saved.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
    }

    /**
     * Get all alert rules with pagination
     */
    @GetMapping
    public ResponseEntity<Page<AlertRuleDTO>> getAllAlertRules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<AlertRule> rules = alertRuleRepository.findAll(pageable);
        
        Page<AlertRuleDTO> dtoPage = rules.map(this::toDTO);
        
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Get alert rule by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlertRuleDTO> getAlertRuleById(@PathVariable Long id) {
        log.debug("Fetching alert rule with ID: {}", id);
        
        return alertRuleRepository.findById(id)
                .map(rule -> ResponseEntity.ok(toDTO(rule)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get enabled alert rules
     */
    @GetMapping("/enabled")
    public ResponseEntity<List<AlertRuleDTO>> getEnabledAlertRules() {
        log.debug("Fetching all enabled alert rules");
        
        List<AlertRule> rules = alertRuleRepository.findByEnabledTrue();
        List<AlertRuleDTO> dtos = rules.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get alert rules by type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<AlertRuleDTO>> getAlertRulesByType(@PathVariable RuleType type) {
        log.debug("Fetching alert rules of type: {}", type);
        
        List<AlertRule> rules = alertRuleRepository.findByType(type);
        List<AlertRuleDTO> dtos = rules.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get alert rules by severity
     */
    @GetMapping("/severity/{severity}")
    public ResponseEntity<List<AlertRuleDTO>> getAlertRulesBySeverity(@PathVariable Severity severity) {
        log.debug("Fetching alert rules with severity: {}", severity);
        
        List<AlertRule> rules = alertRuleRepository.findBySeverity(severity);
        List<AlertRuleDTO> dtos = rules.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get alert rules by service name
     */
    @GetMapping("/service/{serviceName}")
    public ResponseEntity<List<AlertRuleDTO>> getAlertRulesByService(@PathVariable String serviceName) {
        log.debug("Fetching alert rules for service: {}", serviceName);
        
        List<AlertRule> rules = alertRuleRepository.findByServiceName(serviceName);
        List<AlertRuleDTO> dtos = rules.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    /**
     * Update alert rule
     */
    @PutMapping("/{id}")
    public ResponseEntity<AlertRuleDTO> updateAlertRule(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAlertRuleRequest request) {
        
        log.info("Updating alert rule with ID: {}", id);
        
        return alertRuleRepository.findById(id)
                .map(rule -> {
                    if (request.getName() != null) {
                        rule.setName(request.getName());
                    }
                    if (request.getDescription() != null) {
                        rule.setDescription(request.getDescription());
                    }
                    if (request.getType() != null) {
                        rule.setType(request.getType());
                    }
                    if (request.getSeverity() != null) {
                        rule.setSeverity(request.getSeverity());
                    }
                    if (request.getAnomalyThreshold() != null) {
                        rule.setAnomalyThreshold(request.getAnomalyThreshold());
                    }
                    if (request.getConditions() != null) {
                        rule.setConditions(request.getConditions());
                    }
                    if (request.getServiceName() != null) {
                        rule.setServiceName(request.getServiceName());
                    }
                    if (request.getLogLevel() != null) {
                        rule.setLogLevel(request.getLogLevel());
                    }
                    if (request.getTimeWindowMinutes() != null) {
                        rule.setTimeWindowMinutes(request.getTimeWindowMinutes());
                    }
                    if (request.getThreshold() != null) {
                        rule.setThreshold(request.getThreshold());
                    }
                    if (request.getCooldownMinutes() != null) {
                        rule.setCooldownMinutes(request.getCooldownMinutes());
                    }
                    if (request.getNotifyOnRecovery() != null) {
                        rule.setNotifyOnRecovery(request.getNotifyOnRecovery());
                    }
                    
                    rule.setUpdatedAt(LocalDateTime.now());
                    
                    AlertRule updated = alertRuleRepository.save(rule);
                    log.info("Alert rule updated successfully: {}", id);
                    
                    return ResponseEntity.ok(toDTO(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Enable alert rule
     */
    @PostMapping("/{id}/enable")
    public ResponseEntity<AlertRuleDTO> enableAlertRule(@PathVariable Long id) {
        log.info("Enabling alert rule with ID: {}", id);
        
        return alertRuleRepository.findById(id)
                .map(rule -> {
                    rule.setEnabled(true);
                    rule.setUpdatedAt(LocalDateTime.now());
                    AlertRule updated = alertRuleRepository.save(rule);
                    log.info("Alert rule enabled: {}", id);
                    return ResponseEntity.ok(toDTO(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Disable alert rule
     */
    @PostMapping("/{id}/disable")
    public ResponseEntity<AlertRuleDTO> disableAlertRule(@PathVariable Long id) {
        log.info("Disabling alert rule with ID: {}", id);
        
        return alertRuleRepository.findById(id)
                .map(rule -> {
                    rule.setEnabled(false);
                    rule.setUpdatedAt(LocalDateTime.now());
                    AlertRule updated = alertRuleRepository.save(rule);
                    log.info("Alert rule disabled: {}", id);
                    return ResponseEntity.ok(toDTO(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete alert rule
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlertRule(@PathVariable Long id) {
        log.info("Deleting alert rule with ID: {}", id);
        
        if (!alertRuleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        alertRuleRepository.deleteById(id);
        log.info("Alert rule deleted: {}", id);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Test alert rule (dry run)
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<String> testAlertRule(@PathVariable Long id) {
        log.info("Testing alert rule with ID: {}", id);
        
        return alertRuleRepository.findById(id)
                .map(rule -> {
                    // In a real implementation, this would trigger a test evaluation
                    String message = String.format(
                            "Alert rule '%s' test completed. Rule is %s and configured for %s severity alerts.",
                            rule.getName(),
                            rule.getEnabled() ? "enabled" : "disabled",
                            rule.getSeverity()
                    );
                    return ResponseEntity.ok(message);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Convert AlertRule entity to DTO
     */
    private AlertRuleDTO toDTO(AlertRule rule) {
        return AlertRuleDTO.builder()
                .id(rule.getId())
                .name(rule.getName())
                .description(rule.getDescription())
                .type(rule.getType())
                .severity(rule.getSeverity())
                .enabled(rule.getEnabled())
                .anomalyThreshold(rule.getAnomalyThreshold())
                .conditions(rule.getConditions())
                .serviceName(rule.getServiceName())
                .logLevel(rule.getLogLevel())
                .timeWindowMinutes(rule.getTimeWindowMinutes())
                .threshold(rule.getThreshold())
                .cooldownMinutes(rule.getCooldownMinutes())
                .notifyOnRecovery(rule.getNotifyOnRecovery())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}

// Made with Bob
