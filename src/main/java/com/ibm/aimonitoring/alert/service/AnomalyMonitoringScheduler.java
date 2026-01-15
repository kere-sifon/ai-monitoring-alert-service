package com.ibm.aimonitoring.alert.service;

import com.ibm.aimonitoring.alert.model.AnomalyDetection;
import com.ibm.aimonitoring.alert.repository.AnomalyDetectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Anomaly Monitoring Scheduler
 * 
 * Periodically monitors the anomaly_detections table for new anomalies
 * and triggers alert evaluation through the rule engine.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnomalyMonitoringScheduler {

    private final AnomalyDetectionRepository anomalyDetectionRepository;
    private final AlertRuleEngine alertRuleEngine;

    @Value("${alert.monitoring.enabled:true}")
    private boolean enabled;

    @Value("${alert.monitoring.lookback-minutes:5}")
    private int lookbackMinutes;

    @Value("${alert.monitoring.batch-size:100}")
    private int batchSize;

    private LocalDateTime lastCheckTime = LocalDateTime.now();

    /**
     * Scheduled task to check for new anomalies
     * Runs based on the configured interval (default: every 60 seconds)
     */
    @Scheduled(fixedDelayString = "${alert.monitoring.interval:60000}")
    public void monitorAnomalies() {
        if (!enabled) {
            log.trace("Anomaly monitoring is disabled");
            return;
        }

        try {
            log.debug("Starting anomaly monitoring check");
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime checkFrom = lastCheckTime.minusMinutes(lookbackMinutes);
            
            // Find unprocessed anomalies
            List<AnomalyDetection> unprocessedAnomalies = 
                anomalyDetectionRepository.findUnprocessedAnomalies(checkFrom);

            if (unprocessedAnomalies.isEmpty()) {
                log.debug("No new anomalies found");
            } else {
                log.info("Found {} unprocessed anomalies", unprocessedAnomalies.size());
                processAnomalies(unprocessedAnomalies);
            }

            lastCheckTime = now;
            
        } catch (Exception e) {
            log.error("Error during anomaly monitoring", e);
        }
    }

    /**
     * Process a batch of anomalies
     * 
     * @param anomalies List of anomalies to process
     */
    private void processAnomalies(List<AnomalyDetection> anomalies) {
        int processedCount = 0;
        int alertsTriggered = 0;

        for (AnomalyDetection anomaly : anomalies) {
            try {
                log.debug("Processing anomaly: {}", anomaly.getLogId());
                
                // Evaluate rules for this anomaly
                var triggeredRules = alertRuleEngine.evaluateAnomalyRules(anomaly);
                
                if (!triggeredRules.isEmpty()) {
                    alertsTriggered += triggeredRules.size();
                    log.info("Triggered {} alert(s) for anomaly {}", 
                        triggeredRules.size(), anomaly.getLogId());
                }
                
                processedCount++;
                
                // Respect batch size limit
                if (processedCount >= batchSize) {
                    log.info("Reached batch size limit of {}, stopping processing", batchSize);
                    break;
                }
                
            } catch (Exception e) {
                log.error("Error processing anomaly {}", anomaly.getLogId(), e);
            }
        }

        log.info("Anomaly monitoring complete. Processed: {}, Alerts triggered: {}", 
            processedCount, alertsTriggered);
    }

    /**
     * Manually trigger anomaly monitoring (for testing or admin use)
     */
    public void triggerManualCheck() {
        log.info("Manual anomaly monitoring check triggered");
        monitorAnomalies();
    }

    /**
     * Get monitoring status
     * 
     * @return Map with monitoring information
     */
    public java.util.Map<String, Object> getMonitoringStatus() {
        return java.util.Map.of(
            "enabled", enabled,
            "lastCheckTime", lastCheckTime.toString(),
            "lookbackMinutes", lookbackMinutes,
            "batchSize", batchSize
        );
    }

    /**
     * Check for high-confidence anomalies specifically
     * Can be called on-demand for critical monitoring
     */
    @Scheduled(fixedDelayString = "${alert.monitoring.critical-check-interval:30000}")
    public void monitorCriticalAnomalies() {
        if (!enabled) {
            return;
        }

        try {
            LocalDateTime since = LocalDateTime.now().minusMinutes(lookbackMinutes);
            
            // Find critical anomalies (high confidence and high score)
            List<AnomalyDetection> criticalAnomalies = 
                anomalyDetectionRepository.findCriticalAnomalies(0.8, 0.8, since);

            if (!criticalAnomalies.isEmpty()) {
                log.warn("Found {} critical anomalies requiring immediate attention", 
                    criticalAnomalies.size());
                
                // Process critical anomalies with priority
                for (AnomalyDetection anomaly : criticalAnomalies) {
                    try {
                        alertRuleEngine.evaluateAnomalyRules(anomaly);
                    } catch (Exception e) {
                        log.error("Error processing critical anomaly {}", 
                            anomaly.getLogId(), e);
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error during critical anomaly monitoring", e);
        }
    }

    /**
     * Get statistics about recent anomalies
     * 
     * @return Map with anomaly statistics
     */
    public java.util.Map<String, Object> getAnomalyStatistics() {
        LocalDateTime since = LocalDateTime.now().minusMinutes(lookbackMinutes);
        LocalDateTime now = LocalDateTime.now();
        
        long totalAnomalies = anomalyDetectionRepository
            .countAnomaliesBetween(since, now);
        
        List<AnomalyDetection> unprocessed = anomalyDetectionRepository
            .findUnprocessedAnomalies(since);
        
        List<AnomalyDetection> critical = anomalyDetectionRepository
            .findCriticalAnomalies(0.8, 0.8, since);
        
        return java.util.Map.of(
            "totalAnomalies", totalAnomalies,
            "unprocessedCount", unprocessed.size(),
            "criticalCount", critical.size(),
            "timeWindow", lookbackMinutes + " minutes",
            "lastCheckTime", lastCheckTime.toString()
        );
    }

    /**
     * Reset the last check time (useful for testing or recovery)
     */
    public void resetLastCheckTime() {
        lastCheckTime = LocalDateTime.now().minusMinutes(lookbackMinutes);
        log.info("Last check time reset to {}", lastCheckTime);
    }
}

// Made with Bob
