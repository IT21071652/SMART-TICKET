-- Seed first with: psql "$DATABASE_URL" -f sql/seed_benchmark.sql
-- Run each section separately and compare actual time and buffers in EXPLAIN ANALYZE.

DROP INDEX IF EXISTS idx_tickets_status_category_created;
DROP INDEX IF EXISTS idx_tickets_open_created;
DROP INDEX IF EXISTS idx_tickets_title_trgm;
DROP INDEX IF EXISTS idx_tickets_description_trgm;
ANALYZE tickets;

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, title, category, priority, status, created_at
FROM tickets
WHERE status = 'OPEN' AND category = 'BILLING'
  AND created_at >= NOW() - INTERVAL '30 days'
ORDER BY created_at DESC
LIMIT 50;

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, title, created_at
FROM tickets
WHERE title ILIKE '%invoice%' OR description ILIKE '%invoice%'
ORDER BY created_at DESC
LIMIT 50;

CREATE INDEX idx_tickets_status_category_created
    ON tickets (status, category, created_at DESC);
CREATE INDEX idx_tickets_open_created
    ON tickets (created_at DESC) WHERE status = 'OPEN';
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX idx_tickets_title_trgm ON tickets USING gin (lower(title) gin_trgm_ops);
CREATE INDEX idx_tickets_description_trgm ON tickets USING gin (lower(description) gin_trgm_ops);
ANALYZE tickets;

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, title, category, priority, status, created_at
FROM tickets
WHERE status = 'OPEN' AND category = 'BILLING'
  AND created_at >= NOW() - INTERVAL '30 days'
ORDER BY created_at DESC
LIMIT 50;

EXPLAIN (ANALYZE, BUFFERS)
SELECT id, title, created_at
FROM tickets
WHERE lower(title) LIKE '%invoice%' OR lower(description) LIKE '%invoice%'
ORDER BY created_at DESC
LIMIT 50;
