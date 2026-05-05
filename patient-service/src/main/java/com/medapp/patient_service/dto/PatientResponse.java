package com.medapp.patient_service.dto;

import com.medapp.patient_service.domain.Patient;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PatientResponse(

        Long id,
        String firstName,
        String lastName,
        String identityNumber,
        String email,
        String phone,
        LocalDate dateOfBirth,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy,
        String lastModifiedBy
) {
    public static PatientResponse from(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getIdentityNumber(),
                patient.getEmail(),
                patient.getPhone(),
                patient.getDateOfBirth(),
                patient.getActive(),
                patient.getCreatedAt(),
                patient.getUpdatedAt(),
                patient.getCreatedBy(),
                patient.getLastModifiedBy()
        );
    }
}
