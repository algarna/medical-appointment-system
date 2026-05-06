package com.medapp.patient_service.service;

import com.medapp.patient_service.domain.Patient;
import com.medapp.patient_service.dto.CreatePatientRequest;
import com.medapp.patient_service.dto.PatientResponse;
import com.medapp.patient_service.dto.UpdatePatientRequest;
import com.medapp.patient_service.exception.PatientNotFoundException;
import com.medapp.patient_service.repository.PatientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    public Page<PatientResponse> findAll(Pageable pageable) {
        return patientRepository.findAllByActiveTrue(pageable)
                .map(PatientResponse::from);
    }

    public PatientResponse findById(Long id) {
        return patientRepository.findByIdAndActiveTrue(id)
                .map(PatientResponse::from)
                .orElseThrow(() -> new PatientNotFoundException(id));
    }

    @Transactional
    public PatientResponse create(CreatePatientRequest request) {
        if (patientRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered: " + request.email());
        }
        if (patientRepository.existsByIdentityNumber(request.identityNumber())) {
            throw new IllegalArgumentException("Identity number already registered: " + request.identityNumber());
        }

        Patient patient = Patient.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .identityNumber(request.identityNumber())
                .email(request.email())
                .phone(request.phone())
                .dateOfBirth(request.dateOfBirth())
                .build();

        return PatientResponse.from(patientRepository.save(patient));
    }

    @Transactional
    public PatientResponse update(Long id, UpdatePatientRequest request) {
        Patient patient = patientRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new PatientNotFoundException(id));

        if (request.firstName() != null) patient.setFirstName(request.firstName());
        if (request.lastName() != null) patient.setLastName(request.lastName());
        if (request.phone() != null) patient.setPhone(request.phone());
        if (request.dateOfBirth() != null) patient.setDateOfBirth(request.dateOfBirth());
        if (request.email() != null) {
            if (patientRepository.existsByEmail(request.email())) {
                throw new IllegalArgumentException("Email already registered: " + request.email());
            }
            patient.setEmail(request.email());
        }

        return PatientResponse.from(patientRepository.save(patient));
    }

    @Transactional
    public void delete(Long id) {
        Patient patient = patientRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new PatientNotFoundException(id));

        patient.setActive(false);
        patient.setDeletedAt(java.time.LocalDateTime.now());
        patient.setDeletedBy("system");
        patientRepository.save(patient);
    }
}
