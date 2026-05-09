package com.medapp.appointment_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
class AppointmentServiceApplicationTests {

	@TestConfiguration
	static class TestConfig {

		@Bean
		@ServiceConnection
        PostgreSQLContainer postgreSQLContainer() {
			return new PostgreSQLContainer("postgres:16-alpine");
		}
	}

	@Test
	void contextLoads() {
	}

}
