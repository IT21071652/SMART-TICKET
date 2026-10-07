package com.smartticket.ai;

import java.util.UUID;

public record TicketAnalysis(UUID ticketId, String category, String priority, String sentiment, String summary) {
}
