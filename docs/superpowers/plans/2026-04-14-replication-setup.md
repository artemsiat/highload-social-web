# Replication Setup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Set up PostgreSQL streaming replication (1 primary + 2 replicas) with read/write split in a Spring Boot application using profile-based configuration.

**Architecture:** Two Spring profiles (`standard-db` for single-node, `primary-replica-db` for replication). `AbstractRoutingDataSource` routes read-only transactions to replicas via round-robin, writes to primary. `LazyConnectionDataSourceProxy` wraps the routing datasource so the read-only flag is available at connection-acquisition time. Docker Compose orchestrates the replication topology.

**Tech Stack:** Spring Boot 4.0.3, Java 21, PostgreSQL 16, HikariCP, Docker Compose, Flyway

---

## File Structure

### New files

| File | Responsibility |
|------|---------------|
| `src/main/resources/application-standard-db.yaml` | Single-node DB config (datasource, flyway, docker compose) |
| `src/main/resources/application-primary-replica-db.yaml` | Replication datasource configs, flyway pointing to primary |
| `src/main/java/ru/artem/highload/social/web/config/ReadOnlyRoutingDataSource.java` | `AbstractRoutingDataSource` that routes by `isCurrentTransactionReadOnly()` |
| `src/main/java/ru/artem/highload/social/web/config/ReplicationDataSourceConfig.java` | Creates primary + replica datasources, routing DS, wrapped in `LazyConnectionDataSourceProxy`. Active only under `primary-replica-db` profile |
| `compose-replication.yaml` | Docker Compose: 1 primary + 2 replicas with streaming replication |
| `docker/replication/init-primary.sh` | Init script: creates replication user, updates pg_hba.conf |
| `docker/replication/entrypoint-replica.sh` | Replica entrypoint: waits for primary, runs pg_basebackup, starts postgres |

### Modified files

| File | Change |
|------|--------|
| `src/main/resources/application.yaml` | Remove `spring.datasource.*`, `spring.flyway.*`, `spring.docker.compose.*`. Add `spring.profiles.default: standard-db` |
| `src/main/java/ru/artem/highload/social/web/service/UserService.java` | Add `@Transactional(readOnly = true)` on `search()` and `getProfileById()`, `@Transactional` on `register()` |
| `src/main/java/ru/artem/highload/social/web/service/AuthService.java` | Add `@Transactional(readOnly = true)` on `login()` |
| `src/main/java/ru/artem/highload/social/web/service/TestDataService.java` | Add `@Transactional` on `generate()` |

---

### Task 1: Config Profile Split

**Files:**
- Modify: `src/main/resources/application.yaml`
- Create: `src/main/resources/application-standard-db.yaml`

- [ ] **Step 1: Create `application-standard-db.yaml`**

Move all DB-specific config from `application.yaml` into this new profile file:

```yaml
# src/main/resources/application-standard-db.yaml
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:social}}
    username: ${DB_USER:social_user}
    password: ${DB_PASSWORD:social_pass}
    hikari:
      schema: ${DB_SCHEMA:public}

  flyway:
    enabled: true
    locations: classpath:db/migration
    default-schema: ${DB_SCHEMA:public}

  docker:
    compose:
      enabled: ${DOCKER_ENABLED:true}
      lifecycle-management: start-only
```

- [ ] **Step 2: Clean up `application.yaml`**

Remove `spring.datasource.*`, `spring.flyway.*`, `spring.docker.compose.*`. Add default profile:

```yaml
# src/main/resources/application.yaml
spring:
  application:
    name: highload-social-web
  profiles:
    default: standard-db
  config:
    import:
      - optional:file:./config/secrets.properties

app:
  test-data:
    generate-on-startup: ${TEST_DATA_GENERATE_ON_STARTUP:true}
    startup-record-count: ${TEST_DATA_STARTUP_COUNT:1000000}
    max-allowed-records: ${TEST_DATA_MAX_ALLOWED:5000000}
    default-api-count: ${TEST_DATA_DEFAULT_API_COUNT:1000000}
  jwt:
    secret: ${JWT_SECRET:my-super-secret-key-for-jwt-signing-must-be-at-least-256-bits}
    expiration-ms: ${JWT_EXPIRATION_MS:86400000}

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      percentiles-histogram:
        http.server.requests: true

server:
  port: ${APP_PORT:8075}
```

- [ ] **Step 3: Verify standard-db profile starts correctly**

Run: `./mvnw spring-boot:run`

Expected: App starts, connects to Docker PostgreSQL on port 5433, Flyway runs migrations. Logs show `The following 1 profile is active: "standard-db"`.

- [ ] **Step 4: Verify tests still pass**

Run: `./mvnw test`

Expected: All tests pass. Test profile uses H2 from `application-test.yaml`, unaffected by profile split.

---

### Task 2: Docker Replication Infrastructure

**Files:**
- Create: `docker/replication/init-primary.sh`
- Create: `docker/replication/entrypoint-replica.sh`
- Create: `compose-replication.yaml`

- [ ] **Step 1: Create primary init script**

```bash
#!/bin/bash
# docker/replication/init-primary.sh
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER replicator WITH REPLICATION ENCRYPTED PASSWORD 'replicator_pass';
EOSQL

echo "host replication replicator 0.0.0.0/0 md5" >> "$PGDATA/pg_hba.conf"
```

Make executable: `chmod +x docker/replication/init-primary.sh`

- [ ] **Step 2: Create replica entrypoint script**

```bash
#!/bin/bash
# docker/replication/entrypoint-replica.sh
set -e

until pg_isready -h "$PRIMARY_HOST" -p 5432; do
  echo "Waiting for primary at $PRIMARY_HOST..."
  sleep 2
done

if [ ! -s "$PGDATA/PG_VERSION" ]; then
  echo "Cloning data from primary via pg_basebackup..."
  rm -rf "$PGDATA"/*
  PGPASSWORD="$REPLICATION_PASSWORD" pg_basebackup \
    -h "$PRIMARY_HOST" -p 5432 -U "$REPLICATION_USER" \
    -D "$PGDATA" -Fp -Xs -P -R
  chmod 700 "$PGDATA"
  echo "Base backup completed."
fi

echo "Starting PostgreSQL replica..."
exec postgres "$@"
```

Make executable: `chmod +x docker/replication/entrypoint-replica.sh`

- [ ] **Step 3: Create `compose-replication.yaml`**

```yaml
# compose-replication.yaml
services:
  primary:
    image: postgres:16
    environment:
      POSTGRES_DB: social
      POSTGRES_USER: social_user
      POSTGRES_PASSWORD: social_pass
    ports:
      - "5433:5432"
    volumes:
      - ./docker/replication/init-primary.sh:/docker-entrypoint-initdb.d/init-primary.sh
      - primary_data:/var/lib/postgresql/data
    command: >
      postgres
      -c wal_level=replica
      -c max_wal_senders=4
      -c max_replication_slots=4
      -c wal_keep_size=256MB
      -c hot_standby=on
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U social_user -d social"]
      interval: 5s
      timeout: 3s
      retries: 10

  replica1:
    image: postgres:16
    environment:
      PGDATA: /var/lib/postgresql/data/pgdata
      PRIMARY_HOST: primary
      REPLICATION_USER: replicator
      REPLICATION_PASSWORD: replicator_pass
    ports:
      - "5434:5432"
    volumes:
      - ./docker/replication/entrypoint-replica.sh:/entrypoint-replica.sh
      - replica1_data:/var/lib/postgresql/data
    entrypoint: /entrypoint-replica.sh
    command: ["postgres", "-c", "hot_standby=on"]
    depends_on:
      primary:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "pg_isready"]
      interval: 5s
      timeout: 3s
      retries: 10

  replica2:
    image: postgres:16
    environment:
      PGDATA: /var/lib/postgresql/data/pgdata
      PRIMARY_HOST: primary
      REPLICATION_USER: replicator
      REPLICATION_PASSWORD: replicator_pass
    ports:
      - "5435:5432"
    volumes:
      - ./docker/replication/entrypoint-replica.sh:/entrypoint-replica.sh
      - replica2_data:/var/lib/postgresql/data
    entrypoint: /entrypoint-replica.sh
    command: ["postgres", "-c", "hot_standby=on"]
    depends_on:
      primary:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "pg_isready"]
      interval: 5s
      timeout: 3s
      retries: 10

volumes:
  primary_data:
  replica1_data:
  replica2_data:
```

- [ ] **Step 4: Verify replication cluster starts**

```bash
# Stop any existing single-node setup
docker compose down

# Start replication cluster (fresh)
docker compose -f compose-replication.yaml down -v
docker compose -f compose-replication.yaml up -d

# Wait for all containers
docker compose -f compose-replication.yaml ps
```

Expected: All 3 containers healthy.

- [ ] **Step 5: Verify streaming replication is active**

```bash
# On primary — should show 2 connected replicas
docker compose -f compose-replication.yaml exec primary \
  psql -U social_user -d social -c "SELECT client_addr, state, sync_state FROM pg_stat_replication;"

# On replica1 — should return true
docker compose -f compose-replication.yaml exec replica1 \
  psql -U social_user -d social -c "SELECT pg_is_in_recovery();"

# On replica2 — should return true
docker compose -f compose-replication.yaml exec replica2 \
  psql -U social_user -d social -c "SELECT pg_is_in_recovery();"
```

Expected: `pg_stat_replication` shows 2 rows with `state = streaming`. Both replicas return `pg_is_in_recovery = t`.

---

### Task 3: Read/Write Routing DataSource

**Files:**
- Create: `src/main/java/ru/artem/highload/social/web/config/ReadOnlyRoutingDataSource.java`
- Create: `src/main/java/ru/artem/highload/social/web/config/ReplicationDataSourceConfig.java`
- Create: `src/main/resources/application-primary-replica-db.yaml`
- Modify: `src/main/java/ru/artem/highload/social/web/service/UserService.java`
- Modify: `src/main/java/ru/artem/highload/social/web/service/AuthService.java`
- Modify: `src/main/java/ru/artem/highload/social/web/service/TestDataService.java`

- [ ] **Step 1: Create `ReadOnlyRoutingDataSource`**

```java
package ru.artem.highload.social.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ReadOnlyRoutingDataSource extends AbstractRoutingDataSource {

    private static final String PRIMARY_KEY = "primary";

    private final List<String> replicaKeys;
    private final AtomicInteger counter = new AtomicInteger(0);

    public ReadOnlyRoutingDataSource(List<String> replicaKeys) {
        this.replicaKeys = replicaKeys;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        if (readOnly && !replicaKeys.isEmpty()) {
            int idx = Math.abs(counter.getAndIncrement() % replicaKeys.size());
            String key = replicaKeys.get(idx);
            log.debug("Routing read-only query to: {}", key);
            return key;
        }
        log.debug("Routing query to: {}", PRIMARY_KEY);
        return PRIMARY_KEY;
    }
}
```

**Why `LazyConnectionDataSourceProxy` is essential:** Without it, `DataSourceTransactionManager.doBegin()` acquires a connection from the routing datasource BEFORE setting `TransactionSynchronizationManager.currentTransactionReadOnly`. The routing datasource would always see `readOnly = false`. `LazyConnectionDataSourceProxy` defers actual connection acquisition until the first SQL statement, by which time the read-only flag is set.

- [ ] **Step 2: Create `ReplicationDataSourceConfig`**

```java
package ru.artem.highload.social.web.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@Configuration
@Profile("primary-replica-db")
public class ReplicationDataSourceConfig {

    @Bean
    @ConfigurationProperties("app.datasource.primary")
    public HikariDataSource primaryDataSource() {
        return new HikariDataSource();
    }

    @Bean
    @ConfigurationProperties("app.datasource.replica1")
    public HikariDataSource replica1DataSource() {
        return new HikariDataSource();
    }

    @Bean
    @ConfigurationProperties("app.datasource.replica2")
    public HikariDataSource replica2DataSource() {
        return new HikariDataSource();
    }

    @Bean
    @Primary
    public DataSource dataSource(
            @Qualifier("primaryDataSource") DataSource primary,
            @Qualifier("replica1DataSource") DataSource replica1,
            @Qualifier("replica2DataSource") DataSource replica2) {

        var routing = new ReadOnlyRoutingDataSource(List.of("replica1", "replica2"));
        routing.setTargetDataSources(Map.of(
                "primary", primary,
                "replica1", replica1,
                "replica2", replica2
        ));
        routing.setDefaultTargetDataSource(primary);
        routing.afterPropertiesSet();

        return new LazyConnectionDataSourceProxy(routing);
    }
}
```

**Note:** `routing.afterPropertiesSet()` is called manually because the `ReadOnlyRoutingDataSource` is not a Spring bean itself (it's created inside another bean method), so Spring won't call its lifecycle callbacks.

- [ ] **Step 3: Create `application-primary-replica-db.yaml`**

```yaml
# src/main/resources/application-primary-replica-db.yaml
app:
  datasource:
    primary:
      jdbc-url: jdbc:postgresql://${DB_PRIMARY_HOST:localhost}:${DB_PRIMARY_PORT:5433}/${DB_NAME:social}?ApplicationName=primary
      username: ${DB_USER:social_user}
      password: ${DB_PASSWORD:social_pass}
      pool-name: primary-pool
      maximum-pool-size: ${DB_PRIMARY_POOL_SIZE:10}
    replica1:
      jdbc-url: jdbc:postgresql://${DB_REPLICA1_HOST:localhost}:${DB_REPLICA1_PORT:5434}/${DB_NAME:social}?ApplicationName=replica1
      username: ${DB_USER:social_user}
      password: ${DB_PASSWORD:social_pass}
      pool-name: replica1-pool
      maximum-pool-size: ${DB_REPLICA_POOL_SIZE:10}
    replica2:
      jdbc-url: jdbc:postgresql://${DB_REPLICA2_HOST:localhost}:${DB_REPLICA2_PORT:5435}/${DB_NAME:social}?ApplicationName=replica2
      username: ${DB_USER:social_user}
      password: ${DB_PASSWORD:social_pass}
      pool-name: replica2-pool
      maximum-pool-size: ${DB_REPLICA_POOL_SIZE:10}

spring:
  flyway:
    enabled: true
    url: jdbc:postgresql://${DB_PRIMARY_HOST:localhost}:${DB_PRIMARY_PORT:5433}/${DB_NAME:social}
    user: ${DB_USER:social_user}
    password: ${DB_PASSWORD:social_pass}
    locations: classpath:db/migration
    default-schema: ${DB_SCHEMA:public}
  docker:
    compose:
      enabled: false
```

**Key points:**
- `jdbc-url` (not `url`) is used because `HikariDataSource` properties bind directly via `@ConfigurationProperties`
- `?ApplicationName=...` in JDBC URL makes connections identifiable in `pg_stat_activity`
- Flyway explicitly points to primary via `spring.flyway.url`
- Docker Compose disabled — user manages replication cluster manually

- [ ] **Step 4: Add `@Transactional` to `UserService`**

Add `@Transactional(readOnly = true)` to read methods, `@Transactional` to write methods:

```java
package ru.artem.highload.social.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.artem.highload.social.web.dto.RegisterRequest;
import ru.artem.highload.social.web.dto.UserProfileResponse;
import ru.artem.highload.social.web.entity.UserEntity;
import ru.artem.highload.social.web.exception.LoginAlreadyExistsException;
import ru.artem.highload.social.web.exception.UserNotFoundException;
import ru.artem.highload.social.web.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long register(RegisterRequest request) {
        String hash = passwordEncoder.encode(request.password());
        try {
            return userRepository.createUser(
                    request.login(), hash,
                    request.firstName(), request.lastName(),
                    request.birthDate(), request.gender(),
                    request.interests(), request.city()
            );
        } catch (DuplicateKeyException e) {
            throw new LoginAlreadyExistsException(request.login());
        }
    }

    @Transactional(readOnly = true)
    public List<UserProfileResponse> search(String firstNamePrefix, String lastNamePrefix) {
        return userRepository.searchByFirstNameAndLastNamePrefix(firstNamePrefix, lastNamePrefix)
                .stream()
                .map(user -> new UserProfileResponse(
                        user.getId(), user.getFirstName(), user.getLastName(),
                        user.getBirthDate(), user.getGender(),
                        user.getInterests(), user.getCity()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfileById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        return new UserProfileResponse(
                user.getId(), user.getFirstName(), user.getLastName(),
                user.getBirthDate(), user.getGender(),
                user.getInterests(), user.getCity()
        );
    }
}
```

- [ ] **Step 5: Add `@Transactional(readOnly = true)` to `AuthService`**

```java
package ru.artem.highload.social.web.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.artem.highload.social.web.entity.UserEntity;
import ru.artem.highload.social.web.exception.InvalidCredentialsException;
import ru.artem.highload.social.web.repository.UserRepository;
import ru.artem.highload.social.web.security.JwtUtil;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional(readOnly = true)
    public String login(String login, String password) {
        UserEntity user = userRepository.findByLogin(login)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return jwtUtil.generateToken(user.getId());
    }
}
```

- [ ] **Step 6: Add `@Transactional` to `TestDataService`**

Only the public `generate()` method needs the annotation. The private `insertInBatches()` will participate in the same transaction:

```java
// In TestDataService.java, add import:
import org.springframework.transaction.annotation.Transactional;

// Add annotation to generate():
@Transactional
public GenerateTestDataResponse generate(long requestedCount, long maxAllowed) {
    // ... existing body unchanged ...
}
```

- [ ] **Step 7: Verify tests still pass**

Run: `./mvnw test`

Expected: All tests pass. The `@Transactional` annotations don't affect test behavior with single H2 datasource.

---

### Task 4: End-to-End Verification

- [ ] **Step 1: Start replication cluster**

```bash
# Make sure single-node compose is stopped
docker compose down

# Start fresh replication cluster
docker compose -f compose-replication.yaml down -v
docker compose -f compose-replication.yaml up -d

# Verify all containers are healthy
docker compose -f compose-replication.yaml ps
```

- [ ] **Step 2: Start app with replication profile**

```bash
SPRING_PROFILES_ACTIVE=primary-replica-db ./mvnw spring-boot:run
```

Expected: App starts, Flyway runs migrations on primary, all 3 HikariCP pools initialize (`primary-pool`, `replica1-pool`, `replica2-pool`).

- [ ] **Step 3: Verify read routing to replicas**

```bash
# Search (should go to replica)
curl -s "http://localhost:8075/user/search?firstName=Иван&lastName=Петров" | head -c 200

# Get by ID (should go to replica)
curl -s "http://localhost:8075/user/get/1" | head -c 200

# Check pg_stat_activity on primary — should NOT show read queries from app
docker compose -f compose-replication.yaml exec primary \
  psql -U social_user -d social -c \
  "SELECT application_name, state, query FROM pg_stat_activity WHERE application_name IN ('primary','replica1','replica2');"

# Check pg_stat_activity on replica1
docker compose -f compose-replication.yaml exec replica1 \
  psql -U social_user -d social -c \
  "SELECT application_name, state, query FROM pg_stat_activity WHERE application_name IN ('primary','replica1','replica2');"
```

Expected: Read queries show up on replica connections (application_name = `replica1` or `replica2`), not on `primary`.

- [ ] **Step 4: Verify write routing to primary**

```bash
# Register (should go to primary)
curl -s -X POST http://localhost:8075/user/register \
  -H "Content-Type: application/json" \
  -d '{"login":"test_repl","password":"test123","firstName":"Test","lastName":"Repl","birthDate":"1990-01-01","gender":"MALE","interests":"test","city":"Test"}'
```

Expected: Returns `{"id": N}`. Write goes to primary.

- [ ] **Step 5: Verify standard-db profile still works**

```bash
# Stop replication cluster
docker compose -f compose-replication.yaml down

# Start single-node
docker compose up -d

# Run app with default profile
./mvnw spring-boot:run
```

Expected: App starts with `standard-db` profile, single PostgreSQL, everything works as before.
