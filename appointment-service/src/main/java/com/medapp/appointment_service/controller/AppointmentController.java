package com.medapp.appointment_service.controller;

import com.medapp.appointment_service.domain.AppointmentStatus;
import com.medapp.appointment_service.dto.AppointmentResponse;
import com.medapp.appointment_service.dto.CancelAppointmentRequest;
import com.medapp.appointment_service.dto.CreateAppointmentRequest;
import com.medapp.appointment_service.exception.GlobalExceptionHandler.ErrorResponse;
import com.medapp.appointment_service.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/appointments")
@Tag(name = "Appointments", description = "Appointment management endpoints")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @Operation(summary = "List all active appointments",
            description = "Returns a paginated list of active appointments sorted by appointment date")
    @ApiResponse(responseCode = "200", description = "Appointments retrieved successfully")
    @GetMapping
    public ResponseEntity<Page<AppointmentResponse>> findAll(
            @PageableDefault(size = 20, sort = "appointmentDate") Pageable pageable) {
        return ResponseEntity.ok(appointmentService.findAll(pageable));
    }

    @Operation(summary = "List appointments by patient",
            description = "Returns a paginated list of active appointments for a specific patient")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointments retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "No appointments found for patient")
    })
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<Page<AppointmentResponse>> findByPatient(
            @Parameter(description = "Patient ID") @PathVariable Long patientId,
            @PageableDefault(size = 20, sort = "appointmentDate") Pageable pageable) {
        return ResponseEntity.ok(appointmentService.findByPatient(patientId, pageable));
    }

    @Operation(summary = "List appointments by status",
            description = "Returns a paginated list of active appointments filtered by status")
    @ApiResponse(responseCode = "200", description = "Appointments retrieved successfully")
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<AppointmentResponse>> findByStatus(
            @Parameter(description = "Appointment status: SCHEDULED, CANCELLED, COMPLETED")
            @PathVariable AppointmentStatus status,
            @PageableDefault(size = 20, sort = "appointmentDate") Pageable pageable) {
        return ResponseEntity.ok(appointmentService.findByStatus(status, pageable));
    }

    @Operation(summary = "Get appointment by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment found"),
            @ApiResponse(responseCode = "404", description = "Appointment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> findById(
            @Parameter(description = "Appointment ID") @PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.findById(id));
    }

    @Operation(summary = "Create a new appointment",
            description = "Creates a new appointment. Validates that the patient exists in patient-service")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Appointment created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Duplicate appointment",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Patient not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Patient service unavailable",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<AppointmentResponse> create(
            @Valid @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.create(request));
    }

    @Operation(summary = "Cancel an appointment",
            description = "Cancels a scheduled appointment. Cancellation reason is required")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error or invalid status transition",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Appointment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancel(
            @Parameter(description = "Appointment ID") @PathVariable Long id,
            @Valid @RequestBody CancelAppointmentRequest request) {
        return ResponseEntity.ok(appointmentService.cancel(id, request));
    }

    @Operation(summary = "Complete an appointment",
            description = "Marks a scheduled appointment as completed")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appointment completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid status transition",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Appointment not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{id}/complete")
    public ResponseEntity<AppointmentResponse> complete(
            @Parameter(description = "Appointment ID") @PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.complete(id));
    }
}
