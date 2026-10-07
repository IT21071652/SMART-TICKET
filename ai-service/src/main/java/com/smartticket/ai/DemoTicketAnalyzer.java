package com.smartticket.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "app.llm.mode", havingValue = "demo", matchIfMissing = true)
public class DemoTicketAnalyzer implements TicketAnalyzer {
    private static final Set<String> URGENT_TERMS = Set.of("urgent", "outage", "down", "blocked", "cannot access");
    private static final Set<String> NEGATIVE_TERMS = Set.of("angry", "frustrated", "terrible", "unacceptable", "broken");

    @Override
    public TicketAnalysis analyze(TicketMessage ticket) {
        String text = (ticket.title() + " " + ticket.description()).toLowerCase(Locale.ROOT);
        String category = containsAny(text, "invoice", "payment", "billing", "charged") ? "BILLING"
                : containsAny(text, "login", "password", "account", "access") ? "ACCOUNT"
                : containsAny(text, "bug", "error", "crash", "broken", "outage") ? "TECHNICAL"
                : "GENERAL";
        String priority = containsAny(text, URGENT_TERMS) ? "HIGH"
                : containsAny(text, "soon", "important", "asap") ? "MEDIUM" : "LOW";
        String sentiment = containsAny(text, NEGATIVE_TERMS) ? "NEGATIVE" : "NEUTRAL";
        String summary = ticket.description().replaceAll("\\s+", " ").trim();
        if (summary.length() > 220) {
            summary = summary.substring(0, 217) + "...";
        }
        return new TicketAnalysis(ticket.ticketId(), category, priority, sentiment, summary);
    }

    private static boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) return true;
        }
        return false;
    }

    private static boolean containsAny(String text, Set<String> terms) {
        return terms.stream().anyMatch(text::contains);
    }
}
