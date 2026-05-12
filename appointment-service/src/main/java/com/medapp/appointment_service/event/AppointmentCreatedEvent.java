package com.medapp.appointment_service.event;

import java.time.LocalDateTime;

public record AppointmentCreatedEvent(
        String eventId,
        Long appointmentId,
        Long patientId,
        String doctorName,
        String specialty,
        LocalDateTime appointmentDate,
        String reason,
        LocalDateTime occurredAt
) {
    public static AppointmentCreatedEvent of(
            Long appointmentId,
            Long patientId,
            String doctorName,
            String specialty,
            LocalDateTime appointmentDate,
            String reason) {
        return new AppointmentCreatedEvent(
                java.util.UUID.randomUUID().toString(),
                appointmentId,
                patientId,
                doctorName,
                specialty,
                appointmentDate,
                reason,
                LocalDateTime.now()
        );
    }
}
