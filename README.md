# Smart Support

A local-first support-ticket platform with a React/Redux dashboard, a Spring Boot ticket API, PostgreSQL, Kafka, and a separate asynchronous AI worker.

## Run locally

Requirements: Docker Desktop with Compose enabled.

```powershell
docker compose up --build
```

Open [http://localhost:5173](http://localhost:5173). The first build downloads Maven, Java, Node, PostgreSQL, Kafka, and frontend dependencies. The initial Kafka/PostgreSQL startup can take a minute.

Compose defaults to `LLM_MODE=demo`, so tickets are categorized locally without an API key. This deterministic analyzer is for development only; it is not an LLM. To use a real OpenAI-compatible chat-completions endpoint, create a `.env` file in the project root:

```dotenv
LLM_MODE=openai
LLM_API_KEY=your-api-key
LLM_MODEL=gpt-4o-mini
# Optional for a compatible provider:
LLM_API_URL=https://api.openai.com/v1/chat/completions
```

Then restart the worker:

```powershell
docker compose up --build -d ai-service
```

The key is passed only to the AI worker. When `LLM_MODE=openai`, a missing key or malformed/invalid model output is an explicit processing error; the worker does not silently fall back to demo analysis. Failed analysis is retried three times with exponential backoff and then logged by the dead-letter handler. Spring Kafka creates retry and dead-letter topics from `ticket.created` automatically.

Useful commands:

```powershell
docker compose logs -f ticket-service ai-service
docker compose down
```

Ticket and analysis events flow as `ticket.created` and `ticket.analyzed`. Ticket creation is persisted before the API waits for the Kafka producer acknowledgement; if Kafka is unavailable during that short publish window, the ticket remains stored and the API reports the publish failure. The local setup uses a single Kafka broker and is intended for development, not high availability.

## Services

- `frontend` — React, TypeScript, Redux Toolkit, searchable/filterable ticket desk, ticket form, status/category/priority analytics.
- `ticket-service` — Spring Boot REST API, validation, JPA, Flyway, PostgreSQL, Kafka producer and analysis consumer.
- `ai-service` — Spring Boot Kafka consumer, explicit demo analyzer or configurable OpenAI-compatible structured-JSON analysis, retry and DLT handling.
- `postgres` — ticket store.
- `kafka` — single-node KRaft broker for local development.

API endpoints:

| Method | Path | Description |
| --- | --- | --- |
| `GET` | `/api/tickets` | Paginated tickets; optional `q`, `status`, `category`, `priority`, `page`, and `size` filters |
| `POST` | `/api/tickets` | Create a ticket and publish its analysis request |
| `GET` | `/api/stats` | Ticket totals grouped by status, category, and priority |
| `GET` | `/actuator/health` | Ticket-service health |

## Tests and build

```powershell
mvn test
cd frontend
npm ci
npm run build
```

The Java unit tests use JUnit and Mockito. The ticket API integration test runs PostgreSQL and Kafka with Testcontainers and is skipped when Docker is unavailable. GitHub Actions runs the backend tests and frontend production build.

## Query plan exercise

To load 500,000 synthetic tickets and compare query plans before and after the indexes:

```powershell
docker compose exec -T postgres psql -U ticket -d tickets -f /dev/stdin < sql/seed_benchmark.sql
docker compose exec -T postgres psql -U ticket -d tickets -f /dev/stdin < sql/benchmark.sql
```

`sql/benchmark.sql` compares `EXPLAIN (ANALYZE, BUFFERS)` for filtered listings and keyword search. It removes the relevant indexes for the baseline, then adds composite/partial B-tree indexes and trigram GIN indexes. Capture the actual plans and timings on your machine; query plans depend on data distribution, PostgreSQL version, and hardware.

## Development scope

This is a small local portfolio MVP: it intentionally has no user accounts/JWT, tenant isolation, cloud deployment, or production Kafka/PostgreSQL high availability. Do not expose the Compose ports to an untrusted network. The LLM prompt treats ticket text as untrusted input, but production deployments should add authentication/authorization, rate limits, privacy controls, observability, and a transactional outbox before handling real customer data.
