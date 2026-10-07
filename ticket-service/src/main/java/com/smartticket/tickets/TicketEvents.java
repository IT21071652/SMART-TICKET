package com.smartticket.tickets;

import java.util.UUID;

public final class TicketEvents {
    private TicketEvents() {
    }

    public record TicketCreated(UUID ticketId, String title, String description) {
    }

    public record TicketAnalyzed(UUID ticketId, String category, String priority, String sentiment, String summary) {
    }
}
