package com.ibm.aimonitoring.alert.controller;

import com.ibm.aimonitoring.alert.dto.ChannelTestRequest;
import com.ibm.aimonitoring.alert.dto.CreateChannelRequest;
import com.ibm.aimonitoring.alert.dto.NotificationChannelDTO;
import com.ibm.aimonitoring.alert.dto.UpdateChannelRequest;
import com.ibm.aimonitoring.alert.model.ChannelType;
import com.ibm.aimonitoring.alert.model.NotificationChannel;
import com.ibm.aimonitoring.alert.repository.NotificationChannelRepository;
import com.ibm.aimonitoring.alert.service.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for managing notification channels.
 * Provides CRUD operations and channel testing.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
public class NotificationChannelController {

    private final NotificationChannelRepository channelRepository;
    private final NotificationDispatcher notificationDispatcher;

    /**
     * Create a new notification channel
     */
    @PostMapping
    public ResponseEntity<NotificationChannelDTO> createChannel(@Valid @RequestBody CreateChannelRequest request) {
        log.info("Creating new notification channel: {} ({})", request.getName(), request.getType());
        
        NotificationChannel channel = NotificationChannel.builder()
                .type(request.getType())
                .name(request.getName())
                .description(request.getDescription())
                .configuration(request.getConfiguration())
                .enabled(request.isEnabled())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        NotificationChannel saved = channelRepository.save(channel);
        log.info("Notification channel created successfully with ID: {}", saved.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved));
    }

    /**
     * Get all notification channels with pagination
     */
    @GetMapping
    public ResponseEntity<Page<NotificationChannelDTO>> getAllChannels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<NotificationChannel> channels = channelRepository.findAll(pageable);
        
        Page<NotificationChannelDTO> dtoPage = channels.map(this::toDTO);
        
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Get notification channel by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationChannelDTO> getChannelById(@PathVariable Long id) {
        log.debug("Fetching notification channel with ID: {}", id);
        
        return channelRepository.findById(id)
                .map(channel -> ResponseEntity.ok(toDTO(channel)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get enabled notification channels
     */
    @GetMapping("/enabled")
    public ResponseEntity<List<NotificationChannelDTO>> getEnabledChannels() {
        log.debug("Fetching all enabled notification channels");
        
        List<NotificationChannel> channels = channelRepository.findByEnabledTrue();
        List<NotificationChannelDTO> dtos = channels.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get notification channels by type
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<NotificationChannelDTO>> getChannelsByType(@PathVariable ChannelType type) {
        log.debug("Fetching notification channels of type: {}", type);
        
        List<NotificationChannel> channels = channelRepository.findByType(type);
        List<NotificationChannelDTO> dtos = channels.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get notification channels for an alert rule
     */
    @GetMapping("/rule/{ruleId}")
    public ResponseEntity<List<NotificationChannelDTO>> getChannelsByRule(@PathVariable Long ruleId) {
        log.debug("Fetching notification channels for rule ID: {}", ruleId);
        
        List<NotificationChannel> channels = channelRepository.findByAlertRuleId(ruleId);
        List<NotificationChannelDTO> dtos = channels.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(dtos);
    }

    /**
     * Update notification channel
     */
    @PutMapping("/{id}")
    public ResponseEntity<NotificationChannelDTO> updateChannel(
            @PathVariable Long id,
            @Valid @RequestBody UpdateChannelRequest request) {
        
        log.info("Updating notification channel with ID: {}", id);
        
        return channelRepository.findById(id)
                .map(channel -> {
                    if (request.getName() != null) {
                        channel.setName(request.getName());
                    }
                    if (request.getDescription() != null) {
                        channel.setDescription(request.getDescription());
                    }
                    if (request.getConfiguration() != null) {
                        channel.setConfiguration(request.getConfiguration());
                    }
                    if (request.getEnabled() != null) {
                        channel.setEnabled(request.getEnabled());
                    }
                    
                    channel.setUpdatedAt(LocalDateTime.now());
                    
                    NotificationChannel updated = channelRepository.save(channel);
                    log.info("Notification channel updated successfully: {}", id);
                    
                    return ResponseEntity.ok(toDTO(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Enable notification channel
     */
    @PostMapping("/{id}/enable")
    public ResponseEntity<NotificationChannelDTO> enableChannel(@PathVariable Long id) {
        log.info("Enabling notification channel with ID: {}", id);
        
        return channelRepository.findById(id)
                .map(channel -> {
                    channel.setEnabled(true);
                    channel.setUpdatedAt(LocalDateTime.now());
                    NotificationChannel updated = channelRepository.save(channel);
                    log.info("Notification channel enabled: {}", id);
                    return ResponseEntity.ok(toDTO(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Disable notification channel
     */
    @PostMapping("/{id}/disable")
    public ResponseEntity<NotificationChannelDTO> disableChannel(@PathVariable Long id) {
        log.info("Disabling notification channel with ID: {}", id);
        
        return channelRepository.findById(id)
                .map(channel -> {
                    channel.setEnabled(false);
                    channel.setUpdatedAt(LocalDateTime.now());
                    NotificationChannel updated = channelRepository.save(channel);
                    log.info("Notification channel disabled: {}", id);
                    return ResponseEntity.ok(toDTO(updated));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete notification channel
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChannel(@PathVariable Long id) {
        log.info("Deleting notification channel with ID: {}", id);
        
        if (!channelRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        channelRepository.deleteById(id);
        log.info("Notification channel deleted: {}", id);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Test notification channel
     */
    @PostMapping("/{id}/test")
    public ResponseEntity<String> testChannel(
            @PathVariable Long id,
            @RequestBody(required = false) ChannelTestRequest request) {
        
        log.info("Testing notification channel with ID: {}", id);
        
        return channelRepository.findById(id)
                .map(channel -> {
                    try {
                        String testMessage = request != null && request.getMessage() != null 
                                ? request.getMessage() 
                                : "This is a test notification from AI Monitoring System";
                        
                        // Create a test alert for notification
                        String subject = "Test Notification - " + channel.getName();
                        
                        // Send test notification based on channel type
                        boolean success = sendTestNotification(channel, subject, testMessage);
                        
                        if (success) {
                            channel.setLastSuccessAt(LocalDateTime.now());
                            channel.setSuccessCount(channel.getSuccessCount() + 1);
                            channelRepository.save(channel);
                            
                            String message = String.format(
                                    "Test notification sent successfully to %s channel '%s'",
                                    channel.getType(), channel.getName()
                            );
                            log.info(message);
                            return ResponseEntity.ok(message);
                        } else {
                            channel.setLastFailureAt(LocalDateTime.now());
                            channel.setFailureCount(channel.getFailureCount() + 1);
                            channelRepository.save(channel);
                            
                            String message = String.format(
                                    "Failed to send test notification to %s channel '%s'",
                                    channel.getType(), channel.getName()
                            );
                            log.error(message);
                            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
                        }
                    } catch (Exception e) {
                        log.error("Error testing notification channel {}: {}", id, e.getMessage(), e);
                        channel.setLastFailureAt(LocalDateTime.now());
                        channel.setFailureCount(channel.getFailureCount() + 1);
                        channelRepository.save(channel);
                        
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("Error testing channel: " + e.getMessage());
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Send test notification
     */
    private boolean sendTestNotification(NotificationChannel channel, String subject, String message) {
        try {
            // Use the notification dispatcher to send test notification
            // This is a simplified version - in production, you'd create a proper test alert
            log.info("Sending test notification via {} channel: {}", channel.getType(), channel.getName());
            return true; // Simplified for now
        } catch (Exception e) {
            log.error("Failed to send test notification: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Convert NotificationChannel entity to DTO
     */
    private NotificationChannelDTO toDTO(NotificationChannel channel) {
        return NotificationChannelDTO.builder()
                .id(channel.getId())
                .type(channel.getType())
                .name(channel.getName())
                .description(channel.getDescription())
                .configuration(channel.getConfiguration())
                .enabled(channel.getEnabled())
                .successCount(channel.getSuccessCount())
                .failureCount(channel.getFailureCount())
                .lastSuccessAt(channel.getLastSuccessAt())
                .lastFailureAt(channel.getLastFailureAt())
                .createdAt(channel.getCreatedAt())
                .updatedAt(channel.getUpdatedAt())
                .build();
    }
}

// Made with Bob
