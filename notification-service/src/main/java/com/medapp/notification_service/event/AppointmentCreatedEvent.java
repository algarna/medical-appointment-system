package com.medapp.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AppointmentCreatedEvent(
        String eventId,
        Long appointmentId,
        Long patientId,
        String patientName,
        LocalDateTime appointmentDate,
        String doctorName,
        LocalDateTime occurredAt
) {}
