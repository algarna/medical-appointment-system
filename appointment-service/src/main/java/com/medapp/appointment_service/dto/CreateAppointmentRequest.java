package com.medapp.appointment_service.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateAppointmentRequest(

        @NotNull(message = "Patient ID is required")
        Long patientId,

        @NotBlank(message = "Doctor name is required")
        String doctorName,

        @NotBlank(message = "Specialty is required")
        String specialty,

        @NotNull(message = "Appointment date is required")
        @Future(message = "Appointment date must be in the future")
        LocalDateTime appointmentDate,

        String reason
) {}
