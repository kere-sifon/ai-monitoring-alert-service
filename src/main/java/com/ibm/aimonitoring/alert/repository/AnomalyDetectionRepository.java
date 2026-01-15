package com.ibm.aimonitoring.alert.repository;

import com.ibm.aimonitoring.alert.model.AnomalyDetection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AnomalyDetection entity (read-only)
 * 
 * Reads from ml_service.anomaly_detections table
 */
@Repository
public interface AnomalyDetectionRepository extends JpaRepository<AnomalyDetection, String> {

    /**
     * Find anomalies detected within time range
     */
    @Query("SELECT ad FROM AnomalyDetection ad WHERE ad.detectedAt BETWEEN :startTime AND :endTime " +
           "AND ad.isAnomaly = true ORDER BY ad.detectedAt DESC")
    List<AnomalyDetection> findAnomaliesBetween(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    /**
     * Find high-confidence anomalies within time range
     */
    @Query("SELECT ad FROM AnomalyDetection ad WHERE ad.detectedAt BETWEEN :startTime AND :endTime " +
           "AND ad.isAnomaly = true AND ad.confidence >= :minConfidence " +
           "ORDER BY ad.detectedAt DESC")
    List<AnomalyDetection> findHighConfidenceAnomalies(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        @Param("minConfidence") Double minConfidence
    );

    /**
     * Find anomalies for a specific service
     */
    @Query("SELECT ad FROM AnomalyDetection ad WHERE ad.service = :service " +
           "AND ad.isAnomaly = true AND ad.detectedAt >= :since " +
           "ORDER BY ad.detectedAt DESC")
    List<AnomalyDetection> findAnomaliesByService(
        @Param("service") String service,
        @Param("since") LocalDateTime since
    );

    /**
     * Find recent anomalies not yet processed by alert service
     * (anomalies without corresponding alerts)
     */
    @Query("SELECT ad FROM AnomalyDetection ad WHERE ad.isAnomaly = true " +
           "AND ad.detectedAt >= :since " +
           "AND NOT EXISTS (SELECT 1 FROM Alert a WHERE a.anomalyDetectionId = ad.logId) " +
           "ORDER BY ad.detectedAt ASC")
    List<AnomalyDetection> findUnprocessedAnomalies(@Param("since") LocalDateTime since);

    /**
     * Count anomalies within time range
     */
    @Query("SELECT COUNT(ad) FROM AnomalyDetection ad WHERE ad.detectedAt BETWEEN :startTime AND :endTime " +
           "AND ad.isAnomaly = true")
    long countAnomaliesBetween(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * Count anomalies detected after a specific time
     */
    @Query("SELECT COUNT(ad) FROM AnomalyDetection ad WHERE ad.detectedAt >= :dateTime " +
           "AND ad.isAnomaly = true")
    long countByDetectedAtAfter(@Param("dateTime") LocalDateTime dateTime);
    
    /**
     * Count high-confidence anomalies detected after a specific time
     */
    @Query("SELECT COUNT(ad) FROM AnomalyDetection ad WHERE ad.detectedAt >= :dateTime " +
           "AND ad.isAnomaly = true AND ad.confidence > :confidence")
    long countByDetectedAtAfterAndConfidenceGreaterThan(
        @Param("dateTime") LocalDateTime dateTime,
        @Param("confidence") double confidence
    );
    
    /**
     * Count unprocessed anomalies
     */
    @Query("SELECT COUNT(ad) FROM AnomalyDetection ad WHERE ad.isAnomaly = true " +
           "AND NOT EXISTS (SELECT 1 FROM Alert a WHERE a.anomalyDetectionId = ad.logId)")
    long countUnprocessedAnomalies();

    /**
     * Find critical anomalies (high confidence and high score)
     */
    @Query("SELECT ad FROM AnomalyDetection ad WHERE ad.isAnomaly = true " +
           "AND ad.confidence >= :minConfidence AND ad.anomalyScore >= :minScore " +
           "AND ad.detectedAt >= :since ORDER BY ad.detectedAt DESC")
    List<AnomalyDetection> findCriticalAnomalies(
        @Param("minConfidence") Double minConfidence,
        @Param("minScore") Double minScore,
        @Param("since") LocalDateTime since
    );
}

// Made with Bob
