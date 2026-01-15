package com.ibm.aimonitoring.alert.controller;

import com.ibm.aimonitoring.alert.model.ChannelType;
import com.ibm.aimonitoring.alert.model.NotificationChannel;
import com.ibm.aimonitoring.alert.repository.NotificationChannelRepository;
import com.ibm.aimonitoring.alert.service.NotificationDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
}
