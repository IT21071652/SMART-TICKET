package com.smartticket.ai;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoTicketAnalyzerTest {
    private final DemoTicketAnalyzer analyzer = new DemoTicketAnalyzer();

    @Test
    void classifiesUrgentBillingIssue() {
        TicketAnalysis analysis = analyzer.analyze(new TicketMessage(UUID.randomUUID(),
                "Urgent duplicate payment", "I was charged twice and cannot access the invoice."));

        assertEquals("BILLING", analysis.category());
        assertEquals("HIGH", analysis.priority());
        assertTrue(analysis.summary().length() <= 280);
    }

    @Test
    void limitsSummaryLength() {
        String description = "customer issue ".repeat(40);
        TicketAnalysis analysis = analyzer.analyze(new TicketMessage(UUID.randomUUID(), "Question", description));

        assertEquals(220, analysis.summary().length());
    }
}
