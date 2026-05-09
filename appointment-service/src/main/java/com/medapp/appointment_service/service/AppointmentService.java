package com.medapp.appointment_service.service;

import com.medapp.appointment_service.domain.Appointment;
import com.medapp.appointment_service.domain.AppointmentStatus;
import com.medapp.appointment_service.dto.AppointmentResponse;
import com.medapp.appointment_service.dto.CancelAppointmentRequest;
import com.medapp.appointment_service.dto.CreateAppointmentRequest;
import com.medapp.appointment_service.exception.AppointmentNotFoundException;
import com.medapp.appointment_service.repository.AppointmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientValidationService patientValidationService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PatientValidationService patientValidationService) {
        this.appointmentRepository = appointmentRepository;
        this.patientValidationService = patientValidationService;
    }

    public Page<AppointmentResponse> findAll(Pageable pageable) {
        return appointmentRepository.findAllByActiveTrue(pageable)
                .map(AppointmentResponse::from);
    }

    public Page<AppointmentResponse> findByPatient(Long patientId, Pageable pageable) {
        return appointmentRepository.findAllByPatientIdAndActiveTrue(patientId, pageable)
                .map(AppointmentResponse::from);
    }

    public Page<AppointmentResponse> findByStatus(AppointmentStatus status, Pageable pageable) {
        return appointmentRepository.findAllByStatusAndActiveTrue(status, pageable)
                .map(AppointmentResponse::from);
    }

    public AppointmentResponse findById(Long id) {
        return appointmentRepository.findByIdAndActiveTrue(id)
                .map(AppointmentResponse::from)
                .orElseThrow(() -> new AppointmentNotFoundException(id));
    }

    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        // Validate patient exists in patient-service via HTTP
        patientValidationService.validatePatientExists(request.patientId());

        // Check for duplicate appointment
        boolean duplicate = appointmentRepository
                .existsByPatientIdAndDoctorNameAndAppointmentDateAndStatusNot(
                        request.patientId(),
                        request.doctorName(),
                        request.appointmentDate(),
                        AppointmentStatus.CANCELLED
                );
        if (duplicate) {
            throw new IllegalArgumentException(
                    "An appointment already exists for this patient with Dr. " +
                            request.doctorName() + " at " + request.appointmentDate()
            );
        }

        Appointment appointment = Appointment.builder()
                .patientId(request.patientId())
                .doctorName(request.doctorName())
                .specialty(request.specialty())
                .appointmentDate(request.appointmentDate())
                .reason(request.reason())
                .build();

        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse cancel(Long id, CancelAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));

        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalArgumentException("Appointment is already cancelled");
        }

        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalArgumentException("A completed appointment cannot be cancelled");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancellationReason(request.reason());
        appointment.setCancelledAt(LocalDateTime.now());
        appointment.setCancelledBy("system");

        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse complete(Long id) {
        Appointment appointment = appointmentRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new AppointmentNotFoundException(id));

        if (appointment.getStatus() != AppointmentStatus.SCHEDULED) {
            throw new IllegalArgumentException(
                    "Only scheduled appointments can be completed. Current status: " +
                            appointment.getStatus()
            );
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }
}
