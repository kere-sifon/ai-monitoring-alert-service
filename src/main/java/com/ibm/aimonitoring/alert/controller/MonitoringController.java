package com.ibm.aimonitoring.alert.controller;

import com.ibm.aimonitoring.alert.dto.AlertStatisticsDTO;
import com.ibm.aimonitoring.alert.dto.SystemHealthDTO;
import com.ibm.aimonitoring.alert.model.AlertStatus;
import com.ibm.aimonitoring.alert.model.Severity;
import com.ibm.aimonitoring.alert.repository.AlertRepository;
import com.ibm.aimonitoring.alert.repository.AlertRuleRepository;
import com.ibm.aimonitoring.alert.repository.AnomalyDetectionRepository;
import com.ibm.aimonitoring.alert.repository.NotificationChannelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for monitoring and statistics.
 * Provides system health, alert statistics, and metrics.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final AlertRepository alertRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final NotificationChannelRepository channelRepository;
    private final AnomalyDetectionRepository anomalyDetectionRepository;

    /**
     * Get system health status
     */
    @GetMapping("/health")
    public ResponseEntity<SystemHealthDTO> getSystemHealth() {
        log.debug("Fetching system health status");
        
        try {
            // Check database connectivity
            long totalRules = alertRuleRepository.count();
            long totalChannels = channelRepository.count();
            long totalAlerts = alertRepository.count();
            
            SystemHealthDTO health = SystemHealthDTO.builder()
                    .status("UP")
                    .timestamp(LocalDateTime.now())
                    .database("UP")
                    .scheduler("UP")
                    .totalAlertRules(totalRules)
                    .totalNotificationChannels(totalChannels)
                    .totalAlerts(totalAlerts)
                    .build();
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Error checking system health: {}", e.getMessage(), e);
            
            SystemHealthDTO health = SystemHealthDTO.builder()
                    .status("DOWN")
                    .timestamp(LocalDateTime.now())
                    .database("DOWN")
                    .scheduler("UNKNOWN")
                    .error(e.getMessage())
                    .build();
            
            return ResponseEntity.status(503).body(health);
        }
    }

    /**
     * Get comprehensive alert statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<AlertStatisticsDTO> getAlertStatistics(
            @RequestParam(required = false) Integer hours) {
        
        log.debug("Fetching alert statistics for last {} hours", hours);
        
        LocalDateTime since = hours != null 
                ? LocalDateTime.now().minusHours(hours)
                : LocalDateTime.now().minusDays(7); // Default to 7 days
        
        // Get counts by status
        long openAlerts = alertRepository.countByStatus(AlertStatus.OPEN);
        long acknowledgedAlerts = alertRepository.countByStatus(AlertStatus.ACKNOWLEDGED);
        long resolvedAlerts = alertRepository.countByStatus(AlertStatus.RESOLVED);
        long falsePositiveAlerts = alertRepository.countByStatus(AlertStatus.FALSE_POSITIVE);
        
        // Get counts by severity
        long criticalAlerts = alertRepository.countBySeverity(Severity.CRITICAL);
        long highAlerts = alertRepository.countBySeverity(Severity.HIGH);
        long mediumAlerts = alertRepository.countBySeverity(Severity.MEDIUM);
        long lowAlerts = alertRepository.countBySeverity(Severity.LOW);
        
        // Get recent alerts count
        long recentAlerts = alertRepository.countByCreatedAtAfter(since);
        
        // Get anomaly statistics
        long totalAnomalies = anomalyDetectionRepository.count();
        long recentAnomalies = anomalyDetectionRepository.countByDetectedAtAfter(since);
        long unprocessedAnomalies = anomalyDetectionRepository.countUnprocessedAnomalies();
        
        // Get rule statistics
        long totalRules = alertRuleRepository.count();
        long enabledRules = alertRuleRepository.countByEnabledTrue();
        
        // Get channel statistics
        long totalChannels = channelRepository.count();
        long enabledChannels = channelRepository.countByEnabledTrue();
        
        AlertStatisticsDTO statistics = AlertStatisticsDTO.builder()
                .timestamp(LocalDateTime.now())
                .timeRangeHours(hours != null ? hours : 168) // 7 days = 168 hours
                .totalAlerts(alertRepository.count())
                .openAlerts(openAlerts)
                .acknowledgedAlerts(acknowledgedAlerts)
                .resolvedAlerts(resolvedAlerts)
                .falsePositiveAlerts(falsePositiveAlerts)
                .criticalAlerts(criticalAlerts)
                .highAlerts(highAlerts)
                .mediumAlerts(mediumAlerts)
                .lowAlerts(lowAlerts)
                .recentAlerts(recentAlerts)
                .totalAnomalies(totalAnomalies)
                .recentAnomalies(recentAnomalies)
                .unprocessedAnomalies(unprocessedAnomalies)
                .totalAlertRules(totalRules)
                .enabledAlertRules(enabledRules)
                .totalNotificationChannels(totalChannels)
                .enabledNotificationChannels(enabledChannels)
                .build();
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * Get alert counts by status
     */
    @GetMapping("/alerts/by-status")
    public ResponseEntity<Map<String, Long>> getAlertCountsByStatus() {
        log.debug("Fetching alert counts by status");
        
        Map<String, Long> counts = new HashMap<>();
        counts.put("OPEN", alertRepository.countByStatus(AlertStatus.OPEN));
        counts.put("ACKNOWLEDGED", alertRepository.countByStatus(AlertStatus.ACKNOWLEDGED));
        counts.put("RESOLVED", alertRepository.countByStatus(AlertStatus.RESOLVED));
        counts.put("FALSE_POSITIVE", alertRepository.countByStatus(AlertStatus.FALSE_POSITIVE));
        
        return ResponseEntity.ok(counts);
    }

    /**
     * Get alert counts by severity
     */
    @GetMapping("/alerts/by-severity")
    public ResponseEntity<Map<String, Long>> getAlertCountsBySeverity() {
        log.debug("Fetching alert counts by severity");
        
        Map<String, Long> counts = new HashMap<>();
        counts.put("CRITICAL", alertRepository.countBySeverity(Severity.CRITICAL));
        counts.put("HIGH", alertRepository.countBySeverity(Severity.HIGH));
        counts.put("MEDIUM", alertRepository.countBySeverity(Severity.MEDIUM));
        counts.put("LOW", alertRepository.countBySeverity(Severity.LOW));
        counts.put("INFO", alertRepository.countBySeverity(Severity.INFO));
        
        return ResponseEntity.ok(counts);
    }

    /**
     * Get alert trend data (last 24 hours, hourly)
     */
    @GetMapping("/alerts/trend")
    public ResponseEntity<Map<String, Object>> getAlertTrend() {
        log.debug("Fetching alert trend data");
        
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> trend = new HashMap<>();
        
        // Get hourly counts for last 24 hours
        Map<Integer, Long> hourlyCounts = new HashMap<>();
        for (int i = 23; i >= 0; i--) {
            LocalDateTime hourStart = now.minusHours(i);
            LocalDateTime hourEnd = hourStart.plusHours(1);
            long count = alertRepository.countByCreatedAtBetween(hourStart, hourEnd);
            hourlyCounts.put(i, count);
        }
        
        trend.put("hourly_counts", hourlyCounts);
        trend.put("timestamp", now);
        trend.put("period_hours", 24);
        
        return ResponseEntity.ok(trend);
    }

    /**
     * Get anomaly detection metrics
     */
    @GetMapping("/anomalies/metrics")
    public ResponseEntity<Map<String, Object>> getAnomalyMetrics(
            @RequestParam(defaultValue = "24") int hours) {
        
        log.debug("Fetching anomaly metrics for last {} hours", hours);
        
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        
        long totalAnomalies = anomalyDetectionRepository.countByDetectedAtAfter(since);
        long highConfidenceAnomalies = anomalyDetectionRepository
                .countByDetectedAtAfterAndConfidenceGreaterThan(since, 0.8);
        long unprocessedAnomalies = anomalyDetectionRepository.countUnprocessedAnomalies();
        
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("total_anomalies", totalAnomalies);
        metrics.put("high_confidence_anomalies", highConfidenceAnomalies);
        metrics.put("unprocessed_anomalies", unprocessedAnomalies);
        metrics.put("period_hours", hours);
        metrics.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(metrics);
    }

    /**
     * Get notification channel statistics
     */
    @GetMapping("/channels/statistics")
    public ResponseEntity<Map<String, Object>> getChannelStatistics() {
        log.debug("Fetching notification channel statistics");
        
        long totalChannels = channelRepository.count();
        long enabledChannels = channelRepository.countByEnabledTrue();
        long emailChannels = channelRepository.countByType(
                com.ibm.aimonitoring.alert.model.ChannelType.EMAIL);
        long slackChannels = channelRepository.countByType(
                com.ibm.aimonitoring.alert.model.ChannelType.SLACK);
        long webhookChannels = channelRepository.countByType(
                com.ibm.aimonitoring.alert.model.ChannelType.WEBHOOK);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("total_channels", totalChannels);
        statistics.put("enabled_channels", enabledChannels);
        statistics.put("email_channels", emailChannels);
        statistics.put("slack_channels", slackChannels);
        statistics.put("webhook_channels", webhookChannels);
        statistics.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * Get alert rule statistics
     */
    @GetMapping("/rules/statistics")
    public ResponseEntity<Map<String, Object>> getRuleStatistics() {
        log.debug("Fetching alert rule statistics");
        
        long totalRules = alertRuleRepository.count();
        long enabledRules = alertRuleRepository.countByEnabledTrue();
        long anomalyRules = alertRuleRepository.countByType(
                com.ibm.aimonitoring.alert.model.RuleType.ANOMALY_DETECTION);
        long thresholdRules = alertRuleRepository.countByType(
                com.ibm.aimonitoring.alert.model.RuleType.THRESHOLD);
        long patternRules = alertRuleRepository.countByType(
                com.ibm.aimonitoring.alert.model.RuleType.PATTERN_MATCH);
        
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("total_rules", totalRules);
        statistics.put("enabled_rules", enabledRules);
        statistics.put("anomaly_detection_rules", anomalyRules);
        statistics.put("threshold_rules", thresholdRules);
        statistics.put("pattern_match_rules", patternRules);
        statistics.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(statistics);
    }
}

// Made with Bob
