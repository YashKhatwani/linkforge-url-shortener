# Architecture and Request Flows

## High-level architecture

```mermaid
flowchart TB
    Browser[Browser]
    Frontend[React + Vite]
    API[Spring Boot REST API]
    Service[URL Shortener Service]
    Redis[(Redis)]
    UrlDB[(PostgreSQL: url_mapping)]
    Kafka[(Kafka: url-click-events)]
    Consumer[Kafka Click Consumer]
    Processed[(processed_click_events)]
    Analytics[(url_click_analytics)]

    Browser --> Frontend
    Frontend -->|Create / list / analytics| API
    Browser -->|GET /{shortCode}| API
    API --> Service
    Service -->|Read / fill cache| Redis
    Service -->|Cache miss| UrlDB
    Service -->|Publish click event| Kafka
    Kafka --> Consumer
    Consumer --> Processed
    Consumer --> Analytics
    API -->|Paginated mappings| UrlDB
    API -->|Bulk counts| Analytics
```

## Redirect flow

1. Browser requests `GET /{shortCode}`.
2. The controller calls the URL shortener service.
3. The service looks for `url:{shortCode}` in Redis.
4. On a cache hit, it uses the cached mapping. On a miss, it reads PostgreSQL and fills Redis with a TTL.
5. The service checks the mapping's expiry and redirects to a valid destination.
6. A click event is published for asynchronous analytics processing.

## Click analytics flow

1. A click event containing an event ID, short code, and timestamp is published to Kafka.
2. The consumer receives the event.
3. The analytics repository inserts the event ID into `processed_click_events`.
4. If the ID already exists, the event is skipped.
5. For a new ID, the aggregate in `url_click_analytics` is incremented.
6. The insert and increment run transactionally.

This prevents duplicate counting for a repeated event ID. It does not guarantee delivery under every possible broker, network, or consumer failure.

## URL creation flow

1. The frontend sends `POST /api/urls` with `originalUrl` and optional `customAlias` / `expiresAt`.
2. The controller validates the request.
3. The service generates or checks the alias and persists the mapping.
4. The API returns the short code, short URL, original URL, and expiry.

## Data model

- `url_mapping`: short code, original URL, creation timestamp, optional expiry.
- `url_click_analytics`: aggregate click count and last update timestamp per short code.
- `processed_click_events`: event IDs already applied to analytics.

## Trade-offs and current scope

- Redis cache-aside reduces repeated mapping reads but requires TTL and expiry awareness.
- Kafka keeps analytics off the redirect path, so counts are eventually consistent.
- Idempotency prevents re-applying the same event ID, at the cost of storing processed IDs.
- Pagination limits response size; the listing endpoint fetches page click counts in bulk.
- The single-node local Kafka setup is for development, not high availability.
- Retry/DLT handling, authentication, rate limiting, production deployment, and load testing are future work unless added separately.
