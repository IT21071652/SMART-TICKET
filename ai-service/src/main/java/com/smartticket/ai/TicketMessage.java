package com.smartticket.ai;

import java.util.UUID;

public record TicketMessage(UUID ticketId, String title, String description) {
}
