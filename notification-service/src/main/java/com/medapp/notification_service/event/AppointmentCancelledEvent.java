package com.medapp.notification_service.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AppointmentCancelledEvent(
        String eventId,
        Long appointmentId,
        Long patientId,
        String patientName,
        LocalDateTime appointmentDate,
        String reason,
        LocalDateTime occurredAt
) {}
