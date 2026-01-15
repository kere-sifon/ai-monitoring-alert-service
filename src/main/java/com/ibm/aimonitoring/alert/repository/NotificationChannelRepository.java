package com.ibm.aimonitoring.alert.repository;

import com.ibm.aimonitoring.alert.model.ChannelType;
import com.ibm.aimonitoring.alert.model.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for NotificationChannel entity
 */
@Repository
public interface NotificationChannelRepository extends JpaRepository<NotificationChannel, Long> {

    /**
     * Find all enabled channels for an alert rule
     */
    List<NotificationChannel> findByAlertRuleIdAndEnabledTrue(Long alertRuleId);
    
    /**
     * Find all channels for an alert rule
     */
    List<NotificationChannel> findByAlertRuleId(Long alertRuleId);
    
    /**
     * Find all enabled channels
     */
    List<NotificationChannel> findByEnabledTrue();

    /**
     * Find channels by type
     */
    List<NotificationChannel> findByType(ChannelType type);
    
    /**
     * Find enabled channels by type
     */
    List<NotificationChannel> findByTypeAndEnabledTrue(ChannelType type);

    /**
     * Find channels with high failure rate
     */
    @Query("SELECT nc FROM NotificationChannel nc WHERE nc.enabled = true " +
           "AND nc.failureCount > :threshold ORDER BY nc.failureCount DESC")
    List<NotificationChannel> findChannelsWithHighFailureRate(@Param("threshold") Long threshold);

    /**
     * Count enabled channels for a rule
     */
    long countByAlertRuleIdAndEnabledTrue(Long alertRuleId);
    
    /**
     * Count all enabled channels
     */
    long countByEnabledTrue();
    
    /**
     * Count channels by type
     */
    long countByType(ChannelType type);
}

// Made with Bob
