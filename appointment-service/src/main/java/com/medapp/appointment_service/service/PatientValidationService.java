package com.medapp.appointment_service.service;

import com.medapp.appointment_service.exception.PatientNotFoundException;
import com.medapp.appointment_service.exception.PatientServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@Slf4j
@Service
public class PatientValidationService {

    private final WebClient webClient;

    public PatientValidationService(@Value("${patient.service.url}") String patientServiceUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(patientServiceUrl)
                .build();
    }

    public void validatePatientExists(Long patientId) {
        try {
            webClient.get()
                    .uri("/api/v1/patients/{id}", patientId)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        log.warn("Patient not found with id: {}", patientId);
                        return response.createException()
                                .map(ex -> new PatientNotFoundException(patientId));
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, response -> {
                        log.error("Patient service returned 5xx for patient id: {}", patientId);
                        return response.createException()
                                .map(ex -> new PatientServiceUnavailableException());
                    })
                    .toBodilessEntity()
                    .block();
        } catch (WebClientRequestException ex) {
            // Network error — patient-service is unreachable
            log.error("Patient service is unreachable: {}", ex.getMessage());
            throw new PatientServiceUnavailableException();
        }
    }
}
