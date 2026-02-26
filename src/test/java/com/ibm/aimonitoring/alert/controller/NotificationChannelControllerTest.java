package com.ibm.aimonitoring.alert.controller;

import com.ibm.aimonitoring.alert.dto.CreateChannelRequest;
import com.ibm.aimonitoring.alert.dto.UpdateChannelRequest;
import com.ibm.aimonitoring.alert.model.ChannelType;
import com.ibm.aimonitoring.alert.model.NotificationChannel;
import com.ibm.aimonitoring.alert.repository.NotificationChannelRepository;
import com.ibm.aimonitoring.alert.service.NotificationDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationChannelControllerTest {

    @Mock
    private NotificationChannelRepository channelRepository;

    @Mock
    private NotificationDispatcher notificationDispatcher;

    @InjectMocks
    @Spy
    private NotificationChannelController controller;

    private NotificationChannel testChannel;

    @BeforeEach
    void setUp() {
        testChannel = NotificationChannel.builder()
                .id(1L)
                .name("Test Channel")
                .type(ChannelType.EMAIL)
                .enabled(true)
                .successCount(0L)
                .failureCount(0L)
                .build();
    }

    @Test
    void getChannelById_shouldReturnChannel() {
        when(channelRepository.findById(1L)).thenReturn(Optional.of(testChannel));

        ResponseEntity<?> response = controller.getChannelById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getChannelById_shouldReturn404WhenNotFound() {
        when(channelRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getChannelById(999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getEnabledChannels_shouldReturnList() {
        when(channelRepository.findByEnabledTrue()).thenReturn(List.of(testChannel));

        ResponseEntity<?> response = controller.getEnabledChannels();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getChannelsByType_shouldReturnList() {
        when(channelRepository.findByType(ChannelType.EMAIL)).thenReturn(List.of(testChannel));

        ResponseEntity<?> response = controller.getChannelsByType(ChannelType.EMAIL);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void deleteChannel_shouldReturn204() {
        when(channelRepository.existsById(1L)).thenReturn(true);
        doNothing().when(channelRepository).deleteById(1L);

        ResponseEntity<?> response = controller.deleteChannel(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(channelRepository).deleteById(1L);
    }

    @Test
    void deleteChannel_shouldReturn404WhenNotFound() {
        when(channelRepository.existsById(999L)).thenReturn(false);

        ResponseEntity<?> response = controller.deleteChannel(999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void enableChannel_shouldEnableAndReturn200() {
        when(channelRepository.findById(1L)).thenReturn(Optional.of(testChannel));
        when(channelRepository.save(any(NotificationChannel.class))).thenReturn(testChannel);

        ResponseEntity<?> response = controller.enableChannel(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(channelRepository).save(any(NotificationChannel.class));
    }

    @Test
    void disableChannel_shouldDisableAndReturn200() {
        when(channelRepository.findById(1L)).thenReturn(Optional.of(testChannel));
        when(channelRepository.save(any(NotificationChannel.class))).thenReturn(testChannel);

        ResponseEntity<?> response = controller.disableChannel(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(channelRepository).save(any(NotificationChannel.class));
    }

    @Test
    void testChannel_shouldReturnSuccess() {
        when(channelRepository.findById(1L)).thenReturn(Optional.of(testChannel));
        when(channelRepository.save(any(NotificationChannel.class))).thenReturn(testChannel);

        ResponseEntity<?> response = controller.testChannel(1L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testChannel_shouldReturn404WhenNotFound() {
        when(channelRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.testChannel(999L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void testChannel_shouldReturn500WhenSendFails() throws Exception {
        when(channelRepository.findById(1L)).thenReturn(Optional.of(testChannel));
        when(channelRepository.save(any(NotificationChannel.class))).thenReturn(testChannel);
        doReturn(false).when(controller).sendTestNotification(any(NotificationChannel.class));

        ResponseEntity<?> response = controller.testChannel(1L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat((String) response.getBody()).contains("Failed to send test notification");
        verify(channelRepository).save(any(NotificationChannel.class));
    }

    @Test
    void testChannel_shouldReturn500WhenExceptionOccurs() throws Exception {
        when(channelRepository.findById(1L)).thenReturn(Optional.of(testChannel));
        when(channelRepository.save(any(NotificationChannel.class))).thenReturn(testChannel);
        doThrow(new RuntimeException("Simulated failure")).when(controller).sendTestNotification(any(NotificationChannel.class));

        ResponseEntity<?> response = controller.testChannel(1L, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat((String) response.getBody()).contains("internal error occurred");
    }

    @Test
    void enableChannel_shouldReturn404WhenNotFound() {
        when(channelRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.enableChannel(999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void disableChannel_shouldReturn404WhenNotFound() {
        when(channelRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.disableChannel(999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getChannelsByRule_shouldReturnList() {
        when(channelRepository.findByAlertRuleId(1L)).thenReturn(List.of(testChannel));

        ResponseEntity<?> response = controller.getChannelsByRule(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void createChannel_shouldReturn201() {
        CreateChannelRequest request = CreateChannelRequest.builder()
                .type(ChannelType.EMAIL)
                .name("New Channel")
                .configuration("{}")
                .enabled(true)
                .build();
        when(channelRepository.save(any(NotificationChannel.class))).thenReturn(testChannel);

        ResponseEntity<?> response = controller.createChannel(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(channelRepository).save(any(NotificationChannel.class));
    }

    @Test
    void getAllChannels_shouldReturnPagedResult() {
        Page<NotificationChannel> page = new PageImpl<>(List.of(testChannel));
        when(channelRepository.findAll(any(Pageable.class))).thenReturn(page);

        ResponseEntity<?> response = controller.getAllChannels(0, 20, "createdAt", "DESC");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getAllChannels_shouldSupportAscendingSort() {
        Page<NotificationChannel> page = new PageImpl<>(List.of(testChannel));
        when(channelRepository.findAll(any(Pageable.class))).thenReturn(page);

        ResponseEntity<?> response = controller.getAllChannels(0, 10, "name", "ASC");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateChannel_shouldReturn200() {
        UpdateChannelRequest request = UpdateChannelRequest.builder()
                .name("Updated Name")
                .build();
        when(channelRepository.findById(1L)).thenReturn(Optional.of(testChannel));
        when(channelRepository.save(any(NotificationChannel.class))).thenReturn(testChannel);

        ResponseEntity<?> response = controller.updateChannel(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(channelRepository).save(any(NotificationChannel.class));
    }

    @Test
    void updateChannel_shouldReturn404WhenNotFound() {
        UpdateChannelRequest request = UpdateChannelRequest.builder().name("Updated").build();
        when(channelRepository.findById(999L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.updateChannel(999L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
