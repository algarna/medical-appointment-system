package com.medapp.appointment_service.repository;

import com.medapp.appointment_service.domain.Appointment;
import com.medapp.appointment_service.domain.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Optional<Appointment> findByIdAndActiveTrue(Long id);

    Page<Appointment> findAllByActiveTrue(Pageable pageable);

    Page<Appointment> findAllByPatientIdAndActiveTrue(Long patientId, Pageable pageable);

    Page<Appointment> findAllByStatusAndActiveTrue(AppointmentStatus status, Pageable pageable);

    boolean existsByPatientIdAndDoctorNameAndAppointmentDateAndStatusNot(
            Long patientId,
            String doctorName,
            LocalDateTime appointmentDate,
            AppointmentStatus status
    );
}
