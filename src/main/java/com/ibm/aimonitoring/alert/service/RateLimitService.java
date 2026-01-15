package com.ibm.aimonitoring.alert.service;

import com.ibm.aimonitoring.alert.model.AlertRule;
import com.ibm.aimonitoring.alert.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limit Service
 * 
 * Prevents alert storms by limiting the number of alerts
 * that can be triggered per rule within a time window.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RateLimitService {

    private final AlertRepository alertRepository;

    @Value("${alert.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${alert.rate-limit.max-alerts-per-rule:10}")
    private int maxAlertsPerRule;

    @Value("${alert.rate-limit.time-window-minutes:60}")
    private int timeWindowMinutes;

    @Value("${alert.rate-limit.cooldown-minutes:15}")
    private int cooldownMinutes;

    // Track cooldown periods for rules
    private final Map<Long, LocalDateTime> cooldownMap = new ConcurrentHashMap<>();

    /**
     * Check if an alert should be allowed for a given rule
     * 
     * @param alertRule The alert rule to check
     * @return true if alert is allowed, false if rate limited
     */
    public boolean isAlertAllowed(AlertRule alertRule) {
        if (!enabled) {
            return true;
        }

        Long ruleId = alertRule.getId();

        // Check if rule is in cooldown period
        if (isInCooldown(ruleId)) {
            log.warn("Alert rule {} is in cooldown period", alertRule.getName());
            return false;
        }

        // Count recent alerts for this rule
        LocalDateTime since = LocalDateTime.now().minusMinutes(timeWindowMinutes);
        long recentAlertCount = alertRepository.countByRuleIdSince(ruleId, since);

        if (recentAlertCount >= maxAlertsPerRule) {
            log.warn("Rate limit exceeded for alert rule {}. Count: {}, Limit: {}", 
                alertRule.getName(), recentAlertCount, maxAlertsPerRule);
            
            // Put rule in cooldown
            enterCooldown(ruleId);
            return false;
        }

        return true;
    }

    /**
     * Check if a rule is currently in cooldown
     * 
     * @param ruleId The rule ID
     * @return true if in cooldown
     */
    private boolean isInCooldown(Long ruleId) {
        LocalDateTime cooldownUntil = cooldownMap.get(ruleId);
        
        if (cooldownUntil == null) {
            return false;
        }

        if (LocalDateTime.now().isAfter(cooldownUntil)) {
            // Cooldown expired, remove from map
            cooldownMap.remove(ruleId);
            return false;
        }

        return true;
    }

    /**
     * Put a rule into cooldown period
     * 
     * @param ruleId The rule ID
     */
    private void enterCooldown(Long ruleId) {
        LocalDateTime cooldownUntil = LocalDateTime.now().plusMinutes(cooldownMinutes);
        cooldownMap.put(ruleId, cooldownUntil);
        log.info("Alert rule {} entered cooldown until {}", ruleId, cooldownUntil);
    }

    /**
     * Manually clear cooldown for a rule (admin override)
     * 
     * @param ruleId The rule ID
     */
    public void clearCooldown(Long ruleId) {
        cooldownMap.remove(ruleId);
        log.info("Cooldown cleared for alert rule {}", ruleId);
    }

    /**
     * Get cooldown status for a rule
     * 
     * @param ruleId The rule ID
     * @return Map with cooldown information
     */
    public Map<String, Object> getCooldownStatus(Long ruleId) {
        LocalDateTime cooldownUntil = cooldownMap.get(ruleId);
        
        if (cooldownUntil == null || LocalDateTime.now().isAfter(cooldownUntil)) {
            return Map.of(
                "inCooldown", false,
                "cooldownUntil", ""
            );
        }

        return Map.of(
            "inCooldown", true,
            "cooldownUntil", cooldownUntil.toString()
        );
    }

    /**
     * Get alert count for a rule within the time window
     * 
     * @param ruleId The rule ID
     * @return Alert count
     */
    public long getAlertCount(Long ruleId) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(timeWindowMinutes);
        return alertRepository.countByRuleIdSince(ruleId, since);
    }

    /**
     * Check if rate limiting is enabled
     * 
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get rate limit configuration
     * 
     * @return Map with configuration values
     */
    public Map<String, Object> getConfiguration() {
        return Map.of(
            "enabled", enabled,
            "maxAlertsPerRule", maxAlertsPerRule,
            "timeWindowMinutes", timeWindowMinutes,
            "cooldownMinutes", cooldownMinutes
        );
    }
}

// Made with Bob
