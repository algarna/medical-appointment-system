package com.medapp.notification_service;

import com.medapp.notification_service.event.AppointmentCancelledEvent;
import com.medapp.notification_service.event.AppointmentCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class NotificationServiceApplicationTests {

    @Container
    static final KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void contextLoads() {
    }

    @Test
    void shouldProcessAppointmentCreatedEvent(CapturedOutput output)
            throws ExecutionException, InterruptedException, TimeoutException {

        AppointmentCreatedEvent event = new AppointmentCreatedEvent(
                UUID.randomUUID().toString(),
                1L, 10L, "John Doe",
                LocalDateTime.now().plusDays(7),
                "Dr. García", LocalDateTime.now()
        );

        kafkaTemplate.send("appointment.created", String.valueOf(event.appointmentId()), event)
                .get(5, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(output.toString())
                        .contains("[NOTIFICATION]")
                        .contains("Confirmation email simulated")
                        .contains("appointmentId=1")
        );
    }

    @Test
    void shouldProcessAppointmentCancelledEvent(CapturedOutput output)
            throws ExecutionException, InterruptedException, TimeoutException {

        AppointmentCancelledEvent event = new AppointmentCancelledEvent(
                UUID.randomUUID().toString(),
                2L, 10L, "John Doe",
                LocalDateTime.now().plusDays(3),
                "Patient request", LocalDateTime.now()
        );

        kafkaTemplate.send("appointment.cancelled", String.valueOf(event.appointmentId()), event)
                .get(5, TimeUnit.SECONDS);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(output.toString())
                        .contains("[NOTIFICATION]")
                        .contains("Cancellation email simulated")
                        .contains("appointmentId=2")
        );
    }
}
