package com.ibm.aimonitoring.alert.model;

/**
 * Alert Severity Levels
 */
public enum Severity {
    /**
     * Informational - no action required
     */
    INFO,
    
    /**
     * Low severity - minor issue
     */
    LOW,
    
    /**
     * Medium severity - requires attention
     */
    MEDIUM,
    
    /**
     * High severity - requires immediate attention
     */
    HIGH,
    
    /**
     * Critical - system failure or major issue
     */
    CRITICAL
}

// Made with Bob
