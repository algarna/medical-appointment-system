package com.medapp.appointment_service.exception;

public class AppointmentNotFoundException extends RuntimeException {

    public AppointmentNotFoundException(Long id) {
        super("Appointment not found with id: " + id);
    }
}
