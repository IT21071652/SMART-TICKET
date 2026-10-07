package com.smartticket.tickets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartticket.tickets.TicketEvents.TicketCreated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {
    private final TicketRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public TicketService(TicketRepository repository, KafkaTemplate<String, String> kafkaTemplate,
                         ObjectMapper objectMapper) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public Ticket create(CreateTicketRequest request) {
        Ticket ticket = repository.save(new Ticket(request.title().trim(), request.description().trim(),
                request.customerEmail().trim().toLowerCase()));
        try {
            String event = objectMapper.writeValueAsString(
                    new TicketCreated(ticket.getId(), ticket.getTitle(), ticket.getDescription()));
            kafkaTemplate.send("ticket.created", ticket.getId().toString(), event)
                    .get(10, java.util.concurrent.TimeUnit.SECONDS);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize ticket.created event", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing ticket.created event", exception);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException exception) {
            throw new IllegalStateException("Could not publish ticket.created event", exception);
        }
        return ticket;
    }

    @Transactional(readOnly = true)
    public Page<Ticket> search(String status, String category, String priority, String query, Pageable pageable) {
        return repository.search(blankToNull(status), blankToNull(category), blankToNull(priority),
                blankToNull(query), pageable);
    }

    @Transactional(readOnly = true)
    public TicketStats stats() {
        return new TicketStats(repository.count(), toCounts(repository.countByStatus()),
                toCounts(repository.countByCategory()), toCounts(repository.countByPriority()));
    }

    @Transactional
    public void applyAnalysis(TicketEvents.TicketAnalyzed event) {
        repository.findById(event.ticketId()).ifPresentOrElse(
                ticket -> ticket.applyAnalysis(event.category(), event.priority(), event.sentiment(), event.summary()),
                () -> { throw new IllegalStateException("Analysis references unknown ticket " + event.ticketId()); });
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static java.util.Map<String, Long> toCounts(java.util.List<Object[]> rows) {
        return rows.stream().collect(java.util.stream.Collectors.toMap(
                row -> String.valueOf(row[0]), row -> (Long) row[1]));
    }

    public record CreateTicketRequest(
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 160) String title,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 10000) String description,
            @jakarta.validation.constraints.Email @jakarta.validation.constraints.NotBlank
            @jakarta.validation.constraints.Size(max = 254) String customerEmail) {
    }

    public record TicketStats(long total, java.util.Map<String, Long> byStatus,
                              java.util.Map<String, Long> byCategory, java.util.Map<String, Long> byPriority) {
    }
}
