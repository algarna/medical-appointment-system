package com.medapp.appointment_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
class AppointmentServiceApplicationTests {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer<?> postgres =
			new PostgreSQLContainer<>("postgres:16-alpine");

	@Container
	@ServiceConnection
	static final KafkaContainer kafka =
			new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

	@Test
	void contextLoads() {
	}
}
