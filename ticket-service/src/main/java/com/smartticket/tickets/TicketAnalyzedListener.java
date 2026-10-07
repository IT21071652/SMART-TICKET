package com.smartticket.tickets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartticket.tickets.TicketEvents.TicketAnalyzed;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TicketAnalyzedListener {
    private final ObjectMapper objectMapper;
    private final TicketService service;

    public TicketAnalyzedListener(ObjectMapper objectMapper, TicketService service) {
        this.objectMapper = objectMapper;
        this.service = service;
    }

    @KafkaListener(topics = "ticket.analyzed")
    public void onAnalyzed(String message) throws JsonProcessingException {
        service.applyAnalysis(objectMapper.readValue(message, TicketAnalyzed.class));
    }
}
