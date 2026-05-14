package com.medapp.notification_service.consumer;

import com.medapp.notification_service.event.AppointmentCancelledEvent;
import com.medapp.notification_service.event.AppointmentCreatedEvent;
import com.medapp.notification_service.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventConsumer.class);

    private final NotificationService notificationService;

    public AppointmentEventConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "appointment.created", containerFactory = "createdEventContainerFactory")
    public void onAppointmentCreated(AppointmentCreatedEvent event) {
        log.debug("Received appointment.created | eventId={}", event.eventId());
        notificationService.handleAppointmentCreated(event);
    }

    @KafkaListener(topics = "appointment.cancelled", containerFactory = "cancelledEventContainerFactory")
    public void onAppointmentCancelled(AppointmentCancelledEvent event) {
        log.debug("Received appointment.cancelled | eventId={}", event.eventId());
        notificationService.handleAppointmentCancelled(event);
    }
}
