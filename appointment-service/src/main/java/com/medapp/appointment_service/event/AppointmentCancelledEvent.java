package com.medapp.appointment_service.event;

import java.time.LocalDateTime;

public record AppointmentCancelledEvent(
        String eventId,
        Long appointmentId,
        Long patientId,
        String cancellationReason,
        LocalDateTime occurredAt
) {
    public static AppointmentCancelledEvent of(
            Long appointmentId,
            Long patientId,
            String cancellationReason) {
        return new AppointmentCancelledEvent(
                java.util.UUID.randomUUID().toString(),
                appointmentId,
                patientId,
                cancellationReason,
                LocalDateTime.now()
        );
    }
}
