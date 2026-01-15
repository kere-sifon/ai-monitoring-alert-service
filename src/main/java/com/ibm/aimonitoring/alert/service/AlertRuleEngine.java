package com.ibm.aimonitoring.alert.service;

import com.ibm.aimonitoring.alert.model.AlertRule;
import com.ibm.aimonitoring.alert.model.AnomalyDetection;
import com.ibm.aimonitoring.alert.model.RuleType;
import com.ibm.aimonitoring.alert.repository.AlertRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Alert Rule Engine
 * 
 * Evaluates alert rules against anomalies and other conditions
 * to determine if alerts should be triggered.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AlertRuleEngine {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertService alertService;

    /**
     * Evaluate all enabled rules against an anomaly
     * 
     * @param anomaly The anomaly to evaluate
     * @return List of triggered alert rules
     */
    public List<AlertRule> evaluateAnomalyRules(AnomalyDetection anomaly) {
        log.debug("Evaluating rules for anomaly: {}", anomaly.getLogId());

        List<AlertRule> triggeredRules = new ArrayList<>();

        // Get all enabled anomaly detection rules
        List<AlertRule> rules = alertRuleRepository
            .findByTypeAndEnabledTrue(RuleType.ANOMALY_DETECTION);

        for (AlertRule rule : rules) {
            if (shouldTriggerForAnomaly(rule, anomaly)) {
                log.info("Rule {} triggered for anomaly {}", rule.getName(), anomaly.getLogId());
                triggeredRules.add(rule);
                
                // Create alert
                alertService.createAlertFromAnomaly(anomaly, rule);
            }
        }

        return triggeredRules;
    }

    /**
     * Check if a rule should trigger for an anomaly
     * 
     * @param rule The alert rule
     * @param anomaly The anomaly
     * @return true if rule should trigger
     */
    private boolean shouldTriggerForAnomaly(AlertRule rule, AnomalyDetection anomaly) {
        // Check if anomaly is actually flagged as anomaly
        if (!anomaly.getIsAnomaly()) {
            return false;
        }

        // Check confidence threshold
        if (rule.getAnomalyThreshold() != null) {
            if (anomaly.getConfidence() < rule.getAnomalyThreshold()) {
                log.debug("Anomaly confidence {} below threshold {} for rule {}", 
                    anomaly.getConfidence(), rule.getAnomalyThreshold(), rule.getName());
                return false;
            }
        }

        // Check service filter
        if (rule.getServices() != null && !rule.getServices().isEmpty()) {
            if (anomaly.getService() == null || 
                !rule.getServices().contains(anomaly.getService())) {
                log.debug("Service {} not in rule service list for rule {}", 
                    anomaly.getService(), rule.getName());
                return false;
            }
        }

        // Check log level filter
        if (rule.getLogLevels() != null && !rule.getLogLevels().isEmpty()) {
            if (anomaly.getLevel() == null || 
                !rule.getLogLevels().contains(anomaly.getLevel())) {
                log.debug("Log level {} not in rule level list for rule {}", 
                    anomaly.getLevel(), rule.getName());
                return false;
            }
        }

        return true;
    }

    /**
     * Evaluate a specific rule against an anomaly
     * 
     * @param ruleId The rule ID
     * @param anomaly The anomaly
     * @return true if rule triggers
     */
    public boolean evaluateRule(Long ruleId, AnomalyDetection anomaly) {
        AlertRule rule = alertRuleRepository.findById(ruleId)
            .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));

        if (!rule.getEnabled()) {
            log.debug("Rule {} is disabled", rule.getName());
            return false;
        }

        if (rule.getType() != RuleType.ANOMALY_DETECTION) {
            log.warn("Rule {} is not an anomaly detection rule", rule.getName());
            return false;
        }

        return shouldTriggerForAnomaly(rule, anomaly);
    }

    /**
     * Get all rules that would trigger for an anomaly (without creating alerts)
     * 
     * @param anomaly The anomaly
     * @return List of matching rules
     */
    public List<AlertRule> getMatchingRules(AnomalyDetection anomaly) {
        List<AlertRule> matchingRules = new ArrayList<>();

        List<AlertRule> rules = alertRuleRepository
            .findByTypeAndEnabledTrue(RuleType.ANOMALY_DETECTION);

        for (AlertRule rule : rules) {
            if (shouldTriggerForAnomaly(rule, anomaly)) {
                matchingRules.add(rule);
            }
        }

        return matchingRules;
    }

    /**
     * Test a rule against sample data
     * 
     * @param rule The rule to test
     * @param anomaly Sample anomaly data
     * @return Evaluation result with details
     */
    public RuleEvaluationResult testRule(AlertRule rule, AnomalyDetection anomaly) {
        boolean triggered = shouldTriggerForAnomaly(rule, anomaly);
        
        RuleEvaluationResult result = new RuleEvaluationResult();
        result.setTriggered(triggered);
        result.setRuleName(rule.getName());
        result.setAnomalyId(anomaly.getLogId());
        
        // Add evaluation details
        List<String> checks = new ArrayList<>();
        
        checks.add(String.format("Anomaly flagged: %s", anomaly.getIsAnomaly()));
        
        if (rule.getAnomalyThreshold() != null) {
            checks.add(String.format("Confidence check: %.2f >= %.2f = %s", 
                anomaly.getConfidence(), 
                rule.getAnomalyThreshold(),
                anomaly.getConfidence() >= rule.getAnomalyThreshold()));
        }
        
        if (rule.getServices() != null && !rule.getServices().isEmpty()) {
            checks.add(String.format("Service check: %s in [%s] = %s",
                anomaly.getService(),
                rule.getServices(),
                rule.getServices().contains(anomaly.getService())));
        }
        
        if (rule.getLogLevels() != null && !rule.getLogLevels().isEmpty()) {
            checks.add(String.format("Log level check: %s in [%s] = %s",
                anomaly.getLevel(),
                rule.getLogLevels(),
                rule.getLogLevels().contains(anomaly.getLevel())));
        }
        
        result.setEvaluationDetails(checks);
        
        return result;
    }

    /**
     * Rule evaluation result
     */
    @lombok.Data
    public static class RuleEvaluationResult {
        private boolean triggered;
        private String ruleName;
        private String anomalyId;
        private List<String> evaluationDetails;
    }
}

// Made with Bob
