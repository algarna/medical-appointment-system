package com.medapp.appointment_service.exception;

public class PatientServiceUnavailableException extends RuntimeException {

    public PatientServiceUnavailableException() {
        super("Patient service is currently unavailable");
    }
}
