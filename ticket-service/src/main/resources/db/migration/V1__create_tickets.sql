CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    title VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    customer_email VARCHAR(254) NOT NULL,
    status VARCHAR(24) NOT NULL,
    category VARCHAR(32),
    priority VARCHAR(16),
    sentiment VARCHAR(16),
    summary VARCHAR(280),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_tickets_created_at ON tickets (created_at DESC);
CREATE INDEX idx_tickets_status_category_created ON tickets (status, category, created_at DESC);
CREATE INDEX idx_tickets_open_created ON tickets (created_at DESC) WHERE status = 'OPEN';
