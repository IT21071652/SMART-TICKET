package com.smartticket.tickets;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tickets")
public class Ticket {
    @Id
    private UUID id;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(nullable = false, columnDefinition = "text")
    private String description;
    @Column(name = "customer_email", nullable = false, length = 254)
    private String customerEmail;
    @Column(nullable = false, length = 24)
    private String status;
    @Column(length = 32)
    private String category;
    @Column(length = 16)
    private String priority;
    @Column(length = 16)
    private String sentiment;
    @Column(length = 280)
    private String summary;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Ticket() {
    }

    public Ticket(String title, String description, String customerEmail) {
        this.id = UUID.randomUUID();
        this.title = title;
        this.description = description;
        this.customerEmail = customerEmail;
        this.status = "OPEN";
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCustomerEmail() { return customerEmail; }
    public String getStatus() { return status; }
    public String getCategory() { return category; }
    public String getPriority() { return priority; }
    public String getSentiment() { return sentiment; }
    public String getSummary() { return summary; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void applyAnalysis(String category, String priority, String sentiment, String summary) {
        this.category = category;
        this.priority = priority;
        this.sentiment = sentiment;
        this.summary = summary;
        this.status = "ANALYZED";
    }
}
