# URL Shortener

A URL shortener built with Java and Spring Boot, with click metrics.

## Features

- Create a short link for any http(s) URL, with an optional expiration
- Redirect (`302`) from the short code to the original URL
- Click count and last access time per link
- Redirect lookups cached in Redis (cache-aside, TTL capped by the link's expiration); if Redis is down, the app falls back to the database
- Input validation: only http(s) URLs with a host are accepted
- Interactive API docs with Swagger UI

## Stack

- Java 25, Spring Boot 4.1.1, Maven
- PostgreSQL with Liquibase migrations
- Redis (cache for redirects; rate limiting planned)
- Docker Compose, Testcontainers

## Running locally

Requirements: JDK 25 and Docker.

```bash
./mvnw spring-boot:run
```

Spring Boot starts PostgreSQL and Redis from `compose.yaml` automatically and configures the connections. The API listens on `http://localhost:8080`.

Run the tests (Docker must be running):

```bash
./mvnw test
```

## API

Create a short URL:

```bash
curl -X POST http://localhost:8080/api/urls \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com", "expiresInDays": 7}'
```

```json
{
  "code": "U0IqpKj",
  "shortUrl": "http://localhost:8080/U0IqpKj",
  "targetUrl": "https://example.com",
  "expiresAt": "2026-10-20T14:19:54Z"
}
```

`expiresInDays` is optional (1 to 3650).

Follow a short URL:

```bash
curl -i http://localhost:8080/U0IqpKj
```

| Status | Meaning |
|--------|---------|
| 201    | Short URL created |
| 302    | Redirect to the original URL |
| 400    | Invalid request |
| 404    | Code not found |
| 410    | Short URL expired |

Swagger UI is available at `http://localhost:8080/swagger-ui.html`. It follows redirects automatically, so use `curl` or the browser to check the `302`.

## Roadmap

- GitHub Actions CI
- Asynchronous click counting
- Rate limiting with Bucket4j
- Metrics with Micrometer, Prometheus and Grafana
