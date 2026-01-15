package com.ibm.aimonitoring.alert.repository;

import com.ibm.aimonitoring.alert.model.AlertRule;
import com.ibm.aimonitoring.alert.model.RuleType;
import com.ibm.aimonitoring.alert.model.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for AlertRule entity
 */
@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    /**
     * Find alert rule by name
     */
    Optional<AlertRule> findByName(String name);

    /**
     * Find all enabled alert rules
     */
    List<AlertRule> findByEnabledTrue();

    /**
     * Find all rules by type
     */
    List<AlertRule> findByType(RuleType type);
    
    /**
     * Find enabled rules by type
     */
    List<AlertRule> findByTypeAndEnabledTrue(RuleType type);
    
    /**
     * Find rules by severity
     */
    List<AlertRule> findBySeverity(Severity severity);
    
    /**
     * Find rules by service name
     */
    List<AlertRule> findByServiceName(String serviceName);

    /**
     * Find rules monitoring a specific service
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.enabled = true AND " +
           "(ar.services IS NULL OR ar.services LIKE %:service%)")
    List<AlertRule> findByServiceAndEnabled(@Param("service") String service);

    /**
     * Find anomaly detection rules with confidence threshold
     */
    @Query("SELECT ar FROM AlertRule ar WHERE ar.type = 'ANOMALY_DETECTION' " +
           "AND ar.enabled = true AND ar.anomalyThreshold <= :confidence")
    List<AlertRule> findAnomalyRulesForConfidence(@Param("confidence") Double confidence);

    /**
     * Check if rule name exists
     */
    boolean existsByName(String name);

    /**
     * Count enabled rules
     */
    long countByEnabledTrue();
    
    /**
     * Count rules by type
     */
    long countByType(RuleType type);
}

// Made with Bob
