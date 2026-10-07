package com.smartticket.tickets;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class TicketServiceIntegrationTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tickets").withUsername("ticket").withPassword("ticket");

    @Container
    static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.1"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsTicketInPostgresAndPublishesCreatedEvent() throws Exception {
        String response = mockMvc.perform(post("/api/tickets")
                        .contentType("application/json")
                        .content("""
                            {"title":"Cannot sign in","description":"Password reset link has expired","customerEmail":"sam@example.com"}
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(OBJECT_MAPPER.readTree(response).path("id").asText());

        Map<String, Object> consumerProperties = KafkaTestUtils.consumerProps(
                kafka.getBootstrapServers(), "ticket-service-integration-" + id, "false");
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                consumerProperties, new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            consumer.subscribe(List.of("ticket.created"));
            long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
            boolean found = false;
            while (!found && System.nanoTime() < deadline) {
                for (var record : consumer.poll(Duration.ofMillis(250)).records("ticket.created")) {
                    if (id.toString().equals(record.key())
                            && record.value().contains("\"title\":\"Cannot sign in\"")) {
                        found = true;
                        break;
                    }
                }
            }
            assertTrue(found, "ticket.created event should be available in Kafka");
        }
    }
}
