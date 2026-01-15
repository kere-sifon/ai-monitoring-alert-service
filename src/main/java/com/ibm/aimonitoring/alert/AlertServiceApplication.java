package com.ibm.aimonitoring.alert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Alert Service Application
 * 
 * Monitors anomaly detections and triggers alerts based on configured rules.
 * Supports multiple notification channels: Email, Slack, and Webhooks.
 * 
 * Features:
 * - Rule-based alerting
 * - Multiple notification channels
 * - Alert history and acknowledgment
 * - Scheduled anomaly monitoring
 * - Async notification processing
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@EnableScheduling
public class AlertServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlertServiceApplication.class, args);
    }
}

// Made with Bob
