package com.medapp.notification_service.service;

import com.medapp.notification_service.event.AppointmentCancelledEvent;
import com.medapp.notification_service.event.AppointmentCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService")
class NotificationServiceTest {

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService();
    }

    // ─────────────────────────────────────────────
    // handleAppointmentCreated
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("handleAppointmentCreated")
    class HandleAppointmentCreated {

        @Test
        @DisplayName("should process confirmation event without throwing")
        void shouldProcessWithoutThrowing() {
            AppointmentCreatedEvent event = new AppointmentCreatedEvent(
                    "evt-001", 1L, 10L, "John Doe",
                    LocalDateTime.now().plusDays(7), "Dr. García", LocalDateTime.now()
            );

            assertDoesNotThrow(() -> notificationService.handleAppointmentCreated(event));
        }

        @Test
        @DisplayName("should handle event with null patientName")
        void shouldHandleNullPatientName() {
            AppointmentCreatedEvent event = new AppointmentCreatedEvent(
                    "evt-002", 2L, 20L, null,
                    LocalDateTime.now().plusDays(3), "Dr. López", LocalDateTime.now()
            );

            assertDoesNotThrow(() -> notificationService.handleAppointmentCreated(event));
        }
    }

    // ─────────────────────────────────────────────
    // handleAppointmentCancelled
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("handleAppointmentCancelled")
    class HandleAppointmentCancelled {

        @Test
        @DisplayName("should process cancellation event without throwing")
        void shouldProcessWithoutThrowing() {
            AppointmentCancelledEvent event = new AppointmentCancelledEvent(
                    "evt-003", 3L, 30L, "Jane Doe",
                    LocalDateTime.now().plusDays(1), "Patient request", LocalDateTime.now()
            );

            assertDoesNotThrow(() -> notificationService.handleAppointmentCancelled(event));
        }

        @Test
        @DisplayName("should handle event with null reason")
        void shouldHandleNullReason() {
            AppointmentCancelledEvent event = new AppointmentCancelledEvent(
                    "evt-004", 4L, 40L, "Jane Doe",
                    LocalDateTime.now().plusDays(2), null, LocalDateTime.now()
            );

            assertDoesNotThrow(() -> notificationService.handleAppointmentCancelled(event));
        }
    }
}
