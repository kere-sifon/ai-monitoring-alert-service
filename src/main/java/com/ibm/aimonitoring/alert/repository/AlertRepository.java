package com.ibm.aimonitoring.alert.repository;

import com.ibm.aimonitoring.alert.model.Alert;
import com.ibm.aimonitoring.alert.model.AlertStatus;
import com.ibm.aimonitoring.alert.model.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Alert entity
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /**
     * Find alerts by status
     */
    Page<Alert> findByStatus(AlertStatus status, Pageable pageable);

    /**
     * Find alerts by severity
     */
    Page<Alert> findBySeverity(Severity severity, Pageable pageable);

    /**
     * Find alerts by alert rule
     */
    Page<Alert> findByAlertRuleId(Long alertRuleId, Pageable pageable);

    /**
     * Find alerts by service
     */
    Page<Alert> findByService(String service, Pageable pageable);

    /**
     * Find open alerts
     */
    List<Alert> findByStatus(AlertStatus status);

    /**
     * Find alerts created within time range
     */
    @Query("SELECT a FROM Alert a WHERE a.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY a.createdAt DESC")
    List<Alert> findByCreatedAtBetween(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Find unacknowledged alerts older than specified time
     */
    @Query("SELECT a FROM Alert a WHERE a.status = 'OPEN' " +
           "AND a.createdAt < :threshold ORDER BY a.createdAt ASC")
    List<Alert> findUnacknowledgedOlderThan(@Param("threshold") LocalDateTime threshold);

    /**
     * Find alerts by anomaly detection ID
     */
    List<Alert> findByAnomalyDetectionId(String anomalyDetectionId);

    /**
     * Count alerts by status
     */
    long countByStatus(AlertStatus status);

    /**
     * Count alerts by severity
     */
    long countBySeverity(Severity severity);
    
    /**
     * Count alerts triggered after a specific time
     */
    long countByCreatedAtAfter(LocalDateTime dateTime);
    
    /**
     * Count alerts triggered within a time range
     */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Count alerts for a rule within time window
     */
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.alertRule.id = :ruleId " +
           "AND a.createdAt >= :since")
    long countByRuleIdSince(@Param("ruleId") Long ruleId, @Param("since") LocalDateTime since);

    /**
     * Find alerts with failed notifications
     */
    @Query("SELECT a FROM Alert a WHERE a.notificationSent = false " +
           "AND a.notificationFailureCount < :maxRetries " +
           "ORDER BY a.createdAt ASC")
    List<Alert> findPendingNotifications(@Param("maxRetries") int maxRetries);

    /**
     * Find recent alerts for dashboard
     */
    @Query("SELECT a FROM Alert a WHERE a.createdAt >= :since " +
           "ORDER BY a.createdAt DESC")
    List<Alert> findRecentAlerts(@Param("since") LocalDateTime since, Pageable pageable);
}

// Made with Bob
