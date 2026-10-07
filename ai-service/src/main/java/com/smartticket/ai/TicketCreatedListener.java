package com.smartticket.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class TicketCreatedListener {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TicketCreatedListener.class);

    private final ObjectMapper objectMapper;
    private final TicketAnalyzer analyzer;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public TicketCreatedListener(ObjectMapper objectMapper, TicketAnalyzer analyzer,
                                 KafkaTemplate<String, String> kafkaTemplate) {
        this.objectMapper = objectMapper;
        this.analyzer = analyzer;
        this.kafkaTemplate = kafkaTemplate;
    }

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = DltStrategy.FAIL_ON_ERROR)
    @KafkaListener(topics = "ticket.created")
    public void onCreated(String message) throws JsonProcessingException {
        TicketMessage ticket = objectMapper.readValue(message, TicketMessage.class);
        TicketAnalysis analysis = analyzer.analyze(ticket);
        try {
            kafkaTemplate.send("ticket.analyzed", ticket.ticketId().toString(),
                    objectMapper.writeValueAsString(analysis)).get(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing ticket.analyzed event", exception);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException exception) {
            throw new IllegalStateException("Could not publish ticket.analyzed event", exception);
        }
    }

    @DltHandler
    public void onDeadLetter(ConsumerRecord<String, String> record) {
        log.error("Ticket analysis permanently failed; record sent to DLT topic {}: key={}, value={}",
                record.topic(), record.key(), record.value());
    }
}
