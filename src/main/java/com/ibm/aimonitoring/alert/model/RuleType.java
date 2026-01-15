package com.ibm.aimonitoring.alert.model;

/**
 * Alert Rule Types
 */
public enum RuleType {
    /**
     * Trigger alert when anomaly is detected with confidence above threshold
     */
    ANOMALY_DETECTION,
    
    /**
     * Trigger alert when log count exceeds threshold within time window
     */
    THRESHOLD,
    
    /**
     * Trigger alert when specific log pattern is matched
     */
    PATTERN_MATCH,
    
    /**
     * Trigger alert when error rate exceeds threshold
     */
    ERROR_RATE,
    
    /**
     * Custom rule with complex conditions
     */
    CUSTOM
}

// Made with Bob
