package com.medapp.patient_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record CreatePatientRequest(

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Identity number is required")
        String identityNumber,

        @Email(message = "Email must be valid")
        @NotBlank(message = "Email is required")
        String email,

        String phone,

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth
){}
