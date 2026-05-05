package com.medapp.patient_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record UpdatePatientRequest(

        String firstName,

        String lastName,

        @Email(message = "Email must be valid")
        String email,

        String phone,

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth
){}
