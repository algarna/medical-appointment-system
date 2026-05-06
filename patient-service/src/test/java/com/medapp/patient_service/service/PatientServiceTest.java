package com.medapp.patient_service.service;

import com.medapp.patient_service.domain.Patient;
import com.medapp.patient_service.dto.CreatePatientRequest;
import com.medapp.patient_service.dto.PatientResponse;
import com.medapp.patient_service.dto.UpdatePatientRequest;
import com.medapp.patient_service.exception.PatientNotFoundException;
import com.medapp.patient_service.repository.PatientRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatientService")
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private PatientService patientService;

    private Patient patient;

    @BeforeEach
    void setUp() {
        patient = Patient.builder()
                .id(1L)
                .firstName("Ana")
                .lastName("García")
                .identityNumber("12345678A")
                .email("ana@example.com")
                .phone("600000000")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .active(true)
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
        @DisplayName("should return paginated active patients")
        void shouldReturnPaginatedActivePatients() {
            // GIVEN — repository returns a page with one active patient
            PageRequest pageable = PageRequest.of(0, 20);
            Page<Patient> page = new PageImpl<>(List.of(patient), pageable, 1);
            when(patientRepository.findAllByActiveTrue(pageable)).thenReturn(page);

            // WHEN
            Page<PatientResponse> result = patientService.findAll(pageable);

            // THEN — result contains exactly one patient with the expected data
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).email()).isEqualTo("ana@example.com");
            verify(patientRepository).findAllByActiveTrue(pageable);
        }

        @Test
        @DisplayName("should return empty page when no active patients exist")
        void shouldReturnEmptyPage() {
            // GIVEN — repository returns an empty page
            PageRequest pageable = PageRequest.of(0, 20);
            when(patientRepository.findAllByActiveTrue(pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            // WHEN
            Page<PatientResponse> result = patientService.findAll(pageable);

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
        @DisplayName("should return patient when found")
        void shouldReturnPatientWhenFound() {
            // GIVEN — repository finds an active patient with the given id
            when(patientRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(patient));

            // WHEN
            PatientResponse result = patientService.findById(1L);

            // THEN
            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.firstName()).isEqualTo("Ana");
            assertThat(result.email()).isEqualTo("ana@example.com");
        }

        @Test
        @DisplayName("should throw PatientNotFoundException when patient does not exist")
        void shouldThrowWhenNotFound() {
            // GIVEN — repository returns empty, covers both missing ids and soft-deleted patients
            when(patientRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

            // WHEN / THEN — exception message must reference the id for easier debugging
            assertThatThrownBy(() -> patientService.findById(99L))
                    .isInstanceOf(PatientNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ─────────────────────────────────────────────
    // create
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("create")
    class Create {

        private CreatePatientRequest request;

        @BeforeEach
        void setUp() {
            request = new CreatePatientRequest(
                    "Ana", "García", "12345678A",
                    "ana@example.com", "600000000",
                    LocalDate.of(1990, 1, 1)
            );
        }

        @Test
        @DisplayName("should create patient when data is valid")
        void shouldCreatePatient() {
            // GIVEN — no duplicates found for email or identity number
            when(patientRepository.existsByEmail("ana@example.com")).thenReturn(false);
            when(patientRepository.existsByIdentityNumber("12345678A")).thenReturn(false);
            when(patientRepository.save(any(Patient.class))).thenReturn(patient);

            // WHEN
            PatientResponse result = patientService.create(request);

            // THEN — patient is saved exactly once and response contains expected data
            assertThat(result.email()).isEqualTo("ana@example.com");
            assertThat(result.identityNumber()).isEqualTo("12345678A");
            verify(patientRepository, times(1)).save(any(Patient.class));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when email already exists")
        void shouldThrowWhenEmailDuplicated() {
            // GIVEN — email is already registered in the system
            when(patientRepository.existsByEmail("ana@example.com")).thenReturn(true);

            // WHEN / THEN — GlobalExceptionHandler maps this to 409 CONFLICT
            assertThatThrownBy(() -> patientService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("ana@example.com");

            // save must never be called if a duplicate is detected
            verify(patientRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when identity number already exists")
        void shouldThrowWhenIdentityNumberDuplicated() {
            // GIVEN — email is unique but identity number belongs to another patient
            when(patientRepository.existsByEmail("ana@example.com")).thenReturn(false);
            when(patientRepository.existsByIdentityNumber("12345678A")).thenReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> patientService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("12345678A");

            verify(patientRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────
    // update
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should update only non-null fields")
        void shouldUpdateOnlyNonNullFields() {
            // GIVEN — only phone is provided, all other fields are null
            UpdatePatientRequest request = new UpdatePatientRequest(
                    null, null, null, "611111111", null
            );
            when(patientRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(patient));
            when(patientRepository.save(any(Patient.class))).thenReturn(patient);

            // WHEN
            patientService.update(1L, request);

            // THEN — phone is updated, firstName remains unchanged
            assertThat(patient.getPhone()).isEqualTo("611111111");
            assertThat(patient.getFirstName()).isEqualTo("Ana");
        }

        @Test
        @DisplayName("should throw PatientNotFoundException when updating non-existent patient")
        void shouldThrowWhenPatientNotFound() {
            // GIVEN
            when(patientRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> patientService.update(99L, new UpdatePatientRequest(
                    null, null, null, null, null)))
                    .isInstanceOf(PatientNotFoundException.class);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when new email already exists")
        void shouldThrowWhenNewEmailDuplicated() {
            // GIVEN — patient exists but the new email is already taken by another patient
            UpdatePatientRequest request = new UpdatePatientRequest(
                    null, null, "otro@example.com", null, null
            );
            when(patientRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(patient));
            when(patientRepository.existsByEmail("otro@example.com")).thenReturn(true);

            // WHEN / THEN
            assertThatThrownBy(() -> patientService.update(1L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("otro@example.com");

            verify(patientRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────
    // delete
    // ─────────────────────────────────────────────
    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should soft delete patient by setting active to false")
        void shouldSoftDeletePatient() {
            // GIVEN
            when(patientRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(patient));
            when(patientRepository.save(any(Patient.class))).thenReturn(patient);

            // WHEN
            patientService.delete(1L);

            // THEN — record is updated, never physically removed
            assertThat(patient.getActive()).isFalse();
            assertThat(patient.getDeletedAt()).isNotNull();
            assertThat(patient.getDeletedBy()).isEqualTo("system");
            verify(patientRepository).save(patient);
            verify(patientRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should throw PatientNotFoundException when deleting non-existent patient")
        void shouldThrowWhenPatientNotFound() {
            // GIVEN
            when(patientRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

            // WHEN / THEN
            assertThatThrownBy(() -> patientService.delete(99L))
                    .isInstanceOf(PatientNotFoundException.class);

            verify(patientRepository, never()).save(any());
        }
    }
}
