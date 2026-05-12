package com.medapp.appointment_service.service;

import com.medapp.appointment_service.domain.Appointment;
import com.medapp.appointment_service.domain.AppointmentStatus;
import com.medapp.appointment_service.dto.AppointmentResponse;
import com.medapp.appointment_service.dto.CancelAppointmentRequest;
import com.medapp.appointment_service.dto.CreateAppointmentRequest;
import com.medapp.appointment_service.exception.AppointmentNotFoundException;
import com.medapp.appointment_service.exception.PatientServiceUnavailableException;
import com.medapp.appointment_service.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentService")
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientValidationService patientValidationService;

    @Mock
    private AppointmentEventProducer eventProducer;

    @InjectMocks
    private AppointmentService appointmentService;

    private Appointment appointment;
    private final LocalDateTime futureDate = LocalDateTime.now().plusDays(7);

    @BeforeEach
    void setUp() {
        appointment = Appointment.builder()
                .id(1L)
                .patientId(10L)
                .doctorName("Dr. García")
                .specialty("Cardiology")
                .appointmentDate(futureDate)
                .status(AppointmentStatus.SCHEDULED)
                .reason("Annual checkup")
                .active(true)
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─────────────────────────────────────────────
    // findAll
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("should return paginated active appointments")
        void shouldReturnPaginatedActiveAppointments() {
            // GIVEN — repository returns a page with one active appointment
            PageRequest pageable = PageRequest.of(0, 20);
            Page<Appointment> page = new PageImpl<>(List.of(appointment), pageable, 1);
            when(appointmentRepository.findAllByActiveTrue(pageable)).thenReturn(page);

            // WHEN
            Page<AppointmentResponse> result = appointmentService.findAll(pageable);

            // THEN
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).doctorName()).isEqualTo("Dr. García");
            verify(appointmentRepository).findAllByActiveTrue(pageable);
        }

        @Test
        @DisplayName("should return empty page when no active appointments exist")
        void shouldReturnEmptyPage() {
            // GIVEN — repository returns an empty page
            PageRequest pageable = PageRequest.of(0, 20);
            when(appointmentRepository.findAllByActiveTrue(pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            // WHEN
            Page<AppointmentResponse> result = appointmentService.findAll(pageable);

            // THEN
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }

    // ─────────────────────────────────────────────
    // findById
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should return appointment when found")
        void shouldReturnAppointmentWhenFound() {
            // GIVEN
            when(appointmentRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(appointment));

            // WHEN
            AppointmentResponse result = appointmentService.findById(1L);

            // THEN
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.doctorName()).isEqualTo("Dr. García");
            assertThat(result.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        }

        @Test
        @DisplayName("should throw AppointmentNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            // GIVEN
            when(appointmentRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> appointmentService.findById(99L))
                    .isInstanceOf(AppointmentNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ─────────────────────────────────────────────
    // create
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("create")
    class Create {

        private CreateAppointmentRequest request;

        @BeforeEach
        void setUp() {
            request = new CreateAppointmentRequest(
                    10L, "Dr. García", "Cardiology", futureDate, "Annual checkup"
            );
        }

        @Test
        @DisplayName("should create appointment when patient exists and no duplicate")
        void shouldCreateAppointment() {
            // GIVEN — patient exists and no duplicate found
            doNothing().when(patientValidationService).validatePatientExists(10L);
            doNothing().when(eventProducer).publishAppointmentCreated(any());
            when(appointmentRepository
                    .existsByPatientIdAndDoctorNameAndAppointmentDateAndStatusNot(
                            10L, "Dr. García", futureDate, AppointmentStatus.CANCELLED))
                    .thenReturn(false);
            when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

            // WHEN
            AppointmentResponse result = appointmentService.create(request);

            // THEN
            assertThat(result.patientId()).isEqualTo(10L);
            assertThat(result.status()).isEqualTo(AppointmentStatus.SCHEDULED);
            verify(appointmentRepository, times(1)).save(any(Appointment.class));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when duplicate appointment exists")
        void shouldThrowWhenDuplicate() {
            // GIVEN — patient exists but a duplicate appointment is detected
            doNothing().when(patientValidationService).validatePatientExists(10L);
            when(appointmentRepository
                    .existsByPatientIdAndDoctorNameAndAppointmentDateAndStatusNot(
                            10L, "Dr. García", futureDate, AppointmentStatus.CANCELLED))
                    .thenReturn(true);

            // WHEN / THEN — save must never be called if duplicate is detected
            assertThatThrownBy(() -> appointmentService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Dr. García");

            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw PatientServiceUnavailableException when patient service is down")
        void shouldThrowWhenPatientServiceUnavailable() {
            // GIVEN — patient service is unreachable
            doThrow(new PatientServiceUnavailableException())
                    .when(patientValidationService).validatePatientExists(10L);

            // WHEN / THEN — no further processing if patient service is unavailable
            assertThatThrownBy(() -> appointmentService.create(request))
                    .isInstanceOf(PatientServiceUnavailableException.class);

            verify(appointmentRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────
    // cancel
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("cancel")
    class Cancel {

        private CancelAppointmentRequest cancelRequest;

        @BeforeEach
        void setUp() {
            cancelRequest = new CancelAppointmentRequest("Patient request");
        }

        @Test
        @DisplayName("should cancel a scheduled appointment")
        void shouldCancelScheduledAppointment() {
            // GIVEN
            doNothing().when(eventProducer).publishAppointmentCancelled(any());
            when(appointmentRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(appointment));
            when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

            // WHEN
            appointmentService.cancel(1L, cancelRequest);

            // THEN — status is CANCELLED and cancellation metadata is set
            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
            assertThat(appointment.getCancellationReason()).isEqualTo("Patient request");
            assertThat(appointment.getCancelledAt()).isNotNull();
            assertThat(appointment.getCancelledBy()).isEqualTo("system");
            verify(appointmentRepository).save(appointment);
        }

        @Test
        @DisplayName("should throw when appointment is already cancelled")
        void shouldThrowWhenAlreadyCancelled() {
            // GIVEN — appointment is already in CANCELLED status
            appointment.setStatus(AppointmentStatus.CANCELLED);
            when(appointmentRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(appointment));

            // WHEN / THEN
            assertThatThrownBy(() -> appointmentService.cancel(1L, cancelRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already cancelled");

            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when appointment is already completed")
        void shouldThrowWhenAlreadyCompleted() {
            // GIVEN — completed appointments cannot be cancelled
            appointment.setStatus(AppointmentStatus.COMPLETED);
            when(appointmentRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(appointment));

            // WHEN / THEN
            assertThatThrownBy(() -> appointmentService.cancel(1L, cancelRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("completed");

            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AppointmentNotFoundException when appointment does not exist")
        void shouldThrowWhenNotFound() {
            // GIVEN
            when(appointmentRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> appointmentService.cancel(99L, cancelRequest))
                    .isInstanceOf(AppointmentNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────
    // complete
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("complete")
    class Complete {

        @Test
        @DisplayName("should complete a scheduled appointment")
        void shouldCompleteScheduledAppointment() {
            // GIVEN
            when(appointmentRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(appointment));
            when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

            // WHEN
            appointmentService.complete(1L);

            // THEN
            assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
            verify(appointmentRepository).save(appointment);
        }

        @Test
        @DisplayName("should throw when trying to complete a cancelled appointment")
        void shouldThrowWhenCancelled() {
            // GIVEN — cancelled appointments cannot be completed
            appointment.setStatus(AppointmentStatus.CANCELLED);
            when(appointmentRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(appointment));

            // WHEN / THEN
            assertThatThrownBy(() -> appointmentService.complete(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("CANCELLED");

            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when trying to complete an already completed appointment")
        void shouldThrowWhenAlreadyCompleted() {
            // GIVEN
            appointment.setStatus(AppointmentStatus.COMPLETED);
            when(appointmentRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(appointment));

            // WHEN / THEN
            assertThatThrownBy(() -> appointmentService.complete(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("COMPLETED");

            verify(appointmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AppointmentNotFoundException when appointment does not exist")
        void shouldThrowWhenNotFound() {
            // GIVEN
            when(appointmentRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> appointmentService.complete(99L))
                    .isInstanceOf(AppointmentNotFoundException.class);
        }
    }
}
