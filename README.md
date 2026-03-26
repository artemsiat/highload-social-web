# Highload Social Web

Minimal social network API with registration, login, and user profile endpoints.

## Prerequisites

- Java 21+
- Docker & Docker Compose

## One-command start (recommended)

The common approach is to keep startup commands in a script or `Makefile`.

Optional: create `.env` to customize ports and DB settings:
```bash
cp .env.example .env
```

Use:
```bash
make dev
```
This starts Docker Desktop (if possible), starts PostgreSQL in Docker, and runs Spring Boot locally.

Useful shortcuts:
```bash
make db     # start only Docker PostgreSQL
make full   # run both DB and app in Docker
make down   # stop Docker services
make reset  # stop services and remove DB volume
```

Inline override example (without editing `.env`):
```bash
APP_PORT=8081 DB_PORT=5434 make dev
```

## Quick Start (recommended for development)

1. Start Docker Desktop app:
```bash
open -a Docker
```
2. Wait until Docker Engine is running, then verify:
```bash
docker info
docker compose version
```
3. Start PostgreSQL in background:
```bash
docker compose up -d
```
Docker PostgreSQL host port comes from `DB_PORT` (default `5433`), so it does not conflict with a local PostgreSQL on `5432`.
4. Run the Spring Boot app locally:
```bash
./mvnw spring-boot:run
```

App URL: `http://localhost:${APP_PORT:-8075}`  
Swagger UI: `http://localhost:${APP_PORT:-8075}/swagger-ui/index.html`

Flyway migrations run automatically on app startup.

## Optional: run both DB and app in Docker

```bash
docker compose --profile full up --build
```

## If Docker commands fail

If you see `Cannot connect to the Docker daemon`, Docker Desktop is not running yet.

Use:
```bash
open -a Docker
docker context use desktop-linux
docker info
```

## Seed User (created by migration)

A test user is inserted automatically by the Liquibase migration:

| Field    | Value         |
|----------|---------------|
| login    | `ivan_petrov` |
| password | `password`    |

Use this account to test login and profile endpoints without registering first.

## API Endpoints

### Login with seed user

```bash
curl -s -X POST http://localhost:${APP_PORT:-8075}/login \
  -H "Content-Type: application/json" \
  -d '{
    "login": "ivan_petrov",
    "password": "password"
  }'
```

Response `200 OK`:
```json
{"token": "eyJhbGciOi..."}
```

### Get seed user profile

```bash
curl -s http://localhost:${APP_PORT:-8075}/user/get/1
```

Response `200 OK`:
```json
{
  "id": 1,
  "firstName": "Иван",
  "lastName": "Петров",
  "birthDate": "1988-03-22",
  "gender": "MALE",
  "interests": "хоккей, рыбалка, программирование",
  "city": "Москва"
}
```

### Register a new user

```bash
curl -s -X POST http://localhost:${APP_PORT:-8075}/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "login": "alexei_sidorov",
    "password": "secret123",
    "firstName": "Алексей",
    "lastName": "Сидоров",
    "birthDate": "1995-07-10",
    "gender": "MALE",
    "interests": "футбол, кино, путешествия",
    "city": "Санкт-Петербург"
  }'
```

Response `201 Created`:
```json
{"id": 2}
```

## Monitoring (Prometheus)

Prometheus metrics are exposed via Spring Boot Actuator + Micrometer.

### Endpoints

| Endpoint | Description |
|---|---|
| `/actuator/prometheus` | Prometheus scrape target |
| `/actuator/metrics` | List all available metrics |
| `/actuator/health` | Health check |

### Available metrics

- **HTTP** — `http_server_requests_*` (latency, count per endpoint/status/method)
- **HikariCP** — `hikaricp_connections_*` (active, idle, pending, usage time)
- **JVM** — memory, GC, threads
- **System** — CPU, file descriptors

### Example

```bash
curl -s http://localhost:${APP_PORT:-8075}/actuator/prometheus | head -20
```

### Prometheus setup

1. Install Prometheus:
```bash
brew install prometheus
```

2. Add a scrape config to `prometheus.yml` (default location: `/opt/homebrew/etc/prometheus.yml`):
```yaml
scrape_configs:
  - job_name: 'highload-social-web'
    metrics_path: '/actuator/prometheus'
    scrape_interval: 5s
    static_configs:
      - targets: ['localhost:8075']
```

3. Start Prometheus:
```bash
brew services start prometheus
```

Prometheus UI: `http://localhost:9090`

### Grafana setup

1. Install Grafana:
```bash
brew install grafana
```

2. Start Grafana:
```bash
brew services start grafana
```

3. Open Grafana UI: `http://localhost:3000` (default login: `admin` / `admin`)

4. Add Prometheus data source:
   - Go to **Connections > Data sources > Add data source**
   - Select **Prometheus**
   - Set URL to `http://localhost:9090`
   - Click **Save & test**

5. Import a dashboard:
   - Go to **Dashboards > Import**
   - Use dashboard ID **4701** (JVM Micrometer) or **6756** (Spring Boot Statistics)
   - Select the Prometheus data source
   - Click **Import**

### Useful Prometheus queries

| Query | Description |
|---|---|
| `rate(http_server_requests_seconds_count[1m])` | Request rate (req/s) |
| `http_server_requests_seconds_max` | Max latency per endpoint |
| `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[1m]))` | p95 latency |
| `hikaricp_connections_active` | Active DB connections |
| `hikaricp_connections_pending` | Pending DB connections |
| `jvm_memory_used_bytes` | JVM memory usage |

## Error Responses

| Status | Meaning                  |
|--------|--------------------------|
| 400    | Validation error         |
| 401    | Invalid login/password   |
| 404    | User not found           |
| 409    | Login already exists     |

## Tech Stack

- Spring Boot 4.0
- PostgreSQL 16
- JdbcTemplate (no ORM)
- Flyway migrations
- BCrypt password hashing
- JWT authentication
- OpenAPI / Swagger UI
- Micrometer + Prometheus
