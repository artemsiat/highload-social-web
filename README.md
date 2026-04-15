# Highload Social Web

Minimal social network API with registration, login, and user profile endpoints.

## Prerequisites

- Java 21+
- Docker & Docker Compose

## How to Run

The project uses **Docker PostgreSQL** as the primary database (port `5433` on the host, to avoid conflicts with local PostgreSQL on `5432`).

### Run from IDE

1. Make sure Docker Desktop is running.
2. Open the project in IntelliJ IDEA (or another IDE).
3. Run `HighloadSocialWebApplication`.

That's it. Spring Boot Docker Compose support automatically starts the PostgreSQL container from `compose.yaml`. The container stays alive across app restarts (`lifecycle-management: start-only`), so you won't lose data or wait for DB startup on every restart.

If you prefer to start the database manually before launching the app:
```bash
make db                   # or: docker compose up -d
```
Then run the app from IDE as usual. Spring Boot detects the already-running container.

### Run as standalone (terminal)

```bash
make dev
```
This starts Docker Desktop (if needed), then runs Spring Boot via Maven.

### Run fully in Docker (DB + app)

```bash
make full
```
Both PostgreSQL and the app run inside Docker. The app image is built from `Dockerfile`.

### Stop / reset

```bash
make down        # stop Docker services (DB data is preserved in a named volume)
make reset       # stop services and remove DB volume (full wipe)
```

### Configuration

Default values are in `.env` and `application.yaml`. Override with environment variables:

| Variable | Default | Description |
|---|---|---|
| `APP_PORT` | `8075` | Application HTTP port |
| `DB_PORT` | `5433` | PostgreSQL host port |
| `DB_NAME` | `social` | Database name |
| `DB_USER` | `social_user` | Database user |
| `DB_PASSWORD` | `social_pass` | Database password |
| `DOCKER_ENABLED` | `true` | Spring Boot Docker Compose support |
| `TEST_DATA_GENERATE_ON_STARTUP` | `true` | Auto-generate test data on startup |
| `TEST_DATA_STARTUP_COUNT` | `1000000` | Target record count for startup generation |

### URLs

| URL | Description |
|---|---|
| `http://localhost:8075` | Application |
| `http://localhost:8075/swagger-ui/index.html` | Swagger UI |
| `http://localhost:8075/actuator/health` | Health check |
| `http://localhost:8075/actuator/prometheus` | Prometheus metrics |

Flyway migrations run automatically on app startup.

## Seed User (created by migration)

A test user is inserted automatically by the Flyway migration:

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
