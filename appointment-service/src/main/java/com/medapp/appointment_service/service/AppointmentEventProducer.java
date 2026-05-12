package com.medapp.appointment_service.service;

import com.medapp.appointment_service.event.AppointmentCancelledEvent;
import com.medapp.appointment_service.event.AppointmentCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AppointmentEventProducer {

    private static final String TOPIC_CREATED = "appointment.created";
    private static final String TOPIC_CANCELLED = "appointment.cancelled";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AppointmentEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishAppointmentCreated(AppointmentCreatedEvent event) {
        kafkaTemplate.send(TOPIC_CREATED, String.valueOf(event.appointmentId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish appointment.created event for appointmentId={}: {}",
                                event.appointmentId(), ex.getMessage());
                    } else {
                        log.info("Published appointment.created event for appointmentId={} to partition={} offset={}",
                                event.appointmentId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    public void publishAppointmentCancelled(AppointmentCancelledEvent event) {
        kafkaTemplate.send(TOPIC_CANCELLED, String.valueOf(event.appointmentId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish appointment.cancelled event for appointmentId={}: {}",
                                event.appointmentId(), ex.getMessage());
                    } else {
                        log.info("Published appointment.cancelled event for appointmentId={} to partition={} offset={}",
                                event.appointmentId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
