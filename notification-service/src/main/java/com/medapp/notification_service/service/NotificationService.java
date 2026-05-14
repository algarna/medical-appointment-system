package com.medapp.notification_service.service;

import com.medapp.notification_service.event.AppointmentCancelledEvent;
import com.medapp.notification_service.event.AppointmentCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void handleAppointmentCreated(AppointmentCreatedEvent event) {
        log.info("[NOTIFICATION] Confirmation email simulated | eventId={} appointmentId={} patientId={} doctor={} date={}",
                event.eventId(), event.appointmentId(), event.patientId(),
                event.doctorName(), event.appointmentDate());
    }

    public void handleAppointmentCancelled(AppointmentCancelledEvent event) {
        log.info("[NOTIFICATION] Cancellation email simulated | eventId={} appointmentId={} patientId={} reason={}",
                event.eventId(), event.appointmentId(), event.patientId(), event.reason());
    }
}
