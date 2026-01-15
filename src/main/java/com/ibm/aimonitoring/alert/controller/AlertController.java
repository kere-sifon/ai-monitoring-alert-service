package com.ibm.aimonitoring.alert.controller;

import com.ibm.aimonitoring.alert.dto.AcknowledgeAlertRequest;
import com.ibm.aimonitoring.alert.dto.AlertDTO;
import com.ibm.aimonitoring.alert.dto.ResolveAlertRequest;
import com.ibm.aimonitoring.alert.model.Alert;
import com.ibm.aimonitoring.alert.model.AlertStatus;
import com.ibm.aimonitoring.alert.model.Severity;
import com.ibm.aimonitoring.alert.service.AlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Alert Controller
 * 
 * REST API for alert management
 */
@RestController
@RequestMapping("/api/v1/alerts")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AlertController {

    private final AlertService alertService;

    /**
     * Get all alerts with pagination
     * 
     * GET /api/v1/alerts?page=0&size=20&sort=createdAt,desc
     */
    @GetMapping
    public ResponseEntity<Page<AlertDTO>> getAllAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(
            sort[1].equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
            sort[0]
        ));
        
        Page<Alert> alerts = alertService.getAllAlerts(pageable);
        Page<AlertDTO> alertDTOs = alerts.map(this::convertToDTO);
        
        return ResponseEntity.ok(alertDTOs);
    }

    /**
     * Get alert by ID
     * 
     * GET /api/v1/alerts/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlertDTO> getAlert(@PathVariable Long id) {
        Alert alert = alertService.getAlert(id);
        return ResponseEntity.ok(convertToDTO(alert));
    }

    /**
     * Get alerts by status
     * 
     * GET /api/v1/alerts/status/{status}
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<AlertDTO>> getAlertsByStatus(
            @PathVariable AlertStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Alert> alerts = alertService.getAlertsByStatus(status, pageable);
        Page<AlertDTO> alertDTOs = alerts.map(this::convertToDTO);
        
        return ResponseEntity.ok(alertDTOs);
    }

    /**
     * Get open alerts
     * 
     * GET /api/v1/alerts/open
     */
    @GetMapping("/open")
    public ResponseEntity<List<AlertDTO>> getOpenAlerts() {
        List<Alert> alerts = alertService.getOpenAlerts();
        List<AlertDTO> alertDTOs = alerts.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(alertDTOs);
    }

    /**
     * Get recent alerts
     * 
     * GET /api/v1/alerts/recent?minutes=60&size=50
     */
    @GetMapping("/recent")
    public ResponseEntity<List<AlertDTO>> getRecentAlerts(
            @RequestParam(defaultValue = "60") int minutes,
            @RequestParam(defaultValue = "50") int size) {
        
        Pageable pageable = PageRequest.of(0, size);
        List<Alert> alerts = alertService.getRecentAlerts(minutes, pageable);
        List<AlertDTO> alertDTOs = alerts.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(alertDTOs);
    }

    /**
     * Get alerts by service
     * 
     * GET /api/v1/alerts/service/{service}
     */
    @GetMapping("/service/{service}")
    public ResponseEntity<Page<AlertDTO>> getAlertsByService(
            @PathVariable String service,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Alert> alerts = alertService.getAlertsByService(service, pageable);
        Page<AlertDTO> alertDTOs = alerts.map(this::convertToDTO);
        
        return ResponseEntity.ok(alertDTOs);
    }

    /**
     * Acknowledge an alert
     * 
     * POST /api/v1/alerts/{id}/acknowledge
     */
    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<AlertDTO> acknowledgeAlert(
            @PathVariable Long id,
            @Valid @RequestBody AcknowledgeAlertRequest request) {
        
        Alert alert = alertService.acknowledgeAlert(id, request.getAcknowledgedBy());
        return ResponseEntity.ok(convertToDTO(alert));
    }

    /**
     * Resolve an alert
     * 
     * POST /api/v1/alerts/{id}/resolve
     */
    @PostMapping("/{id}/resolve")
    public ResponseEntity<AlertDTO> resolveAlert(
            @PathVariable Long id,
            @Valid @RequestBody ResolveAlertRequest request) {
        
        Alert alert = alertService.resolveAlert(id, request.getResolvedBy(), request.getNotes());
        return ResponseEntity.ok(convertToDTO(alert));
    }

    /**
     * Mark alert as false positive
     * 
     * POST /api/v1/alerts/{id}/false-positive
     */
    @PostMapping("/{id}/false-positive")
    public ResponseEntity<AlertDTO> markAsFalsePositive(
            @PathVariable Long id,
            @RequestParam String markedBy) {
        
        Alert alert = alertService.markAsFalsePositive(id, markedBy);
        return ResponseEntity.ok(convertToDTO(alert));
    }

    /**
     * Retry notifications for an alert
     * 
     * POST /api/v1/alerts/{id}/retry-notifications
     */
    @PostMapping("/{id}/retry-notifications")
    public ResponseEntity<Map<String, String>> retryNotifications(@PathVariable Long id) {
        alertService.retryNotifications(id);
        return ResponseEntity.ok(Map.of(
            "message", "Notification retry initiated",
            "alertId", id.toString()
        ));
    }

    /**
     * Get alert statistics
     * 
     * GET /api/v1/alerts/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getStatistics() {
        Map<String, Long> stats = alertService.getAlertStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get alerts by severity
     * 
     * GET /api/v1/alerts/severity/{severity}
     */
    @GetMapping("/severity/{severity}")
    public ResponseEntity<Page<AlertDTO>> getAlertsBySeverity(
            @PathVariable Severity severity,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Alert> alerts = alertService.getAllAlerts(pageable);
        
        // Filter by severity (could be optimized with a repository method)
        Page<Alert> filteredAlerts = alerts.map(alert -> 
            alert.getSeverity() == severity ? alert : null
        );
        
        Page<AlertDTO> alertDTOs = filteredAlerts.map(this::convertToDTO);
        return ResponseEntity.ok(alertDTOs);
    }

    // Helper method to convert Alert to AlertDTO
    private AlertDTO convertToDTO(Alert alert) {
        if (alert == null) {
            return null;
        }
        
        return AlertDTO.builder()
            .id(alert.getId())
            .alertRuleId(alert.getAlertRule().getId())
            .alertRuleName(alert.getAlertRule().getName())
            .status(alert.getStatus())
            .severity(alert.getSeverity())
            .title(alert.getTitle())
            .description(alert.getDescription())
            .anomalyDetectionId(alert.getAnomalyDetectionId())
            .logId(alert.getLogId())
            .service(alert.getService())
            .createdAt(alert.getCreatedAt())
            .updatedAt(alert.getUpdatedAt())
            .acknowledgedAt(alert.getAcknowledgedAt())
            .acknowledgedBy(alert.getAcknowledgedBy())
            .resolvedAt(alert.getResolvedAt())
            .resolvedBy(alert.getResolvedBy())
            .resolutionNotes(alert.getResolutionNotes())
            .notificationSent(alert.getNotificationSent())
            .notificationSentAt(alert.getNotificationSentAt())
            .notificationFailureCount(alert.getNotificationFailureCount())
            .build();
    }
}

// Made with Bob
