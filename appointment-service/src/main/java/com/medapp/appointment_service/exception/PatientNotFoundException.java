package com.medapp.appointment_service.exception;

public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException(Long patientId) {
        super("Patient not found with id: " + patientId);
    }
}
