package com.smartticket.tickets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketServiceTest {
    @Test
    void createPersistsTicketAndPublishesCreatedEvent() throws Exception {
        TicketRepository repository = mock(TicketRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        TicketService service = new TicketService(repository, kafkaTemplate, objectMapper);
        TicketService.CreateTicketRequest request =
                new TicketService.CreateTicketRequest("  Login help  ", "  I am blocked  ", " PERSON@EXAMPLE.COM ");
        when(repository.save(org.mockito.ArgumentMatchers.any(Ticket.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaTemplate.send(eq("ticket.created"), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString())).thenReturn(CompletableFuture.completedFuture(null));

        Ticket created = service.create(request);

        assertEquals("Login help", created.getTitle());
        assertEquals("person@example.com", created.getCustomerEmail());
        verify(kafkaTemplate).send(eq("ticket.created"), eq(created.getId().toString()),
                eq(objectMapper.writeValueAsString(new TicketEvents.TicketCreated(
                        created.getId(), "Login help", "I am blocked"))));
    }

    @Test
    void searchNormalizesEmptyFilters() {
        TicketRepository repository = mock(TicketRepository.class);
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        when(repository.search(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));
        TicketService service = new TicketService(repository, kafkaTemplate, new ObjectMapper());

        service.search(" ", null, "", "  ", PageRequest.of(0, 20));

        verify(repository).search(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any());
    }
}
