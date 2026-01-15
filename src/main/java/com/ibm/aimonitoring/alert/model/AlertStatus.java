package com.ibm.aimonitoring.alert.model;

/**
 * Alert Status
 */
public enum AlertStatus {
    /**
     * Alert is open and requires attention
     */
    OPEN,
    
    /**
     * Alert has been acknowledged but not resolved
     */
    ACKNOWLEDGED,
    
    /**
     * Alert has been resolved
     */
    RESOLVED,
    
    /**
     * Alert was a false positive
     */
    FALSE_POSITIVE
}

// Made with Bob
