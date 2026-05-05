package com.medapp.patient_service.repository;

import com.medapp.patient_service.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByIdAndActiveTrue(Long id);

    Page<Patient> findAllByActiveTrue(Pageable pageable);

    boolean existsByEmail(String email);

    boolean existsByIdentityNumber(String identityNumber);
}
