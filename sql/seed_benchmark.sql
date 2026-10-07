INSERT INTO tickets (
    id, title, description, customer_email, status, category, priority, sentiment, summary, created_at, updated_at
)
SELECT
    md5(series::text)::uuid,
    CASE WHEN series % 10 = 0 THEN 'Invoice payment request ' || series ELSE 'Support request ' || series END,
    CASE WHEN series % 10 = 0 THEN 'Please review invoice payment ' || md5(series::text)
         ELSE 'Generated benchmark ticket body ' || md5(series::text) END,
    'customer' || series || '@example.test',
    CASE WHEN series % 5 = 0 THEN 'OPEN' ELSE 'ANALYZED' END,
    (ARRAY['BILLING', 'TECHNICAL', 'ACCOUNT', 'GENERAL'])[series % 4 + 1],
    (ARRAY['LOW', 'MEDIUM', 'HIGH'])[series % 3 + 1],
    (ARRAY['POSITIVE', 'NEUTRAL', 'NEGATIVE'])[series % 3 + 1],
    'Generated summary ' || series,
    NOW() - ((series % 730)::text || ' hours')::interval,
    NOW() - ((series % 730)::text || ' hours')::interval
FROM generate_series(1, 500000) AS series
ON CONFLICT (id) DO NOTHING;

ANALYZE tickets;
