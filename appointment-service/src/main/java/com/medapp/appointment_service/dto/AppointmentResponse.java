package com.medapp.appointment_service.dto;

import com.medapp.appointment_service.domain.Appointment;
import com.medapp.appointment_service.domain.AppointmentStatus;

import java.time.LocalDateTime;

public record AppointmentResponse(

        Long id,
        Long patientId,
        String doctorName,
        String specialty,
        LocalDateTime appointmentDate,
        AppointmentStatus status,
        String reason,
        String cancellationReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getDoctorName(),
                appointment.getSpecialty(),
                appointment.getAppointmentDate(),
                appointment.getStatus(),
                appointment.getReason(),
                appointment.getCancellationReason(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt()
        );
    }
}
