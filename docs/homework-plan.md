# Social Network Homework Plan (Spring Boot + JdbcTemplate + Flyway)

## Goal
Implement a minimal monolith API (no frontend) with 3 endpoints:
- `POST /user/register`
- `POST /login`
- `GET /user/get/{id}`

Must-have constraints:
- PostgreSQL
- No ORM (use `JdbcTemplate`)
- SQL injection-safe queries (parameterized)
- Secure password storage (hash only)

---

## Recommended Architecture
- `controller` - HTTP request/response mapping
- `service` - business logic
- `repository` - SQL via `JdbcTemplate`
- `config/security` - password encoder, token logic
- `exception` - global error handler

Keep it simple and monolithic.

---

## Phase-by-Phase Plan

## 1) Environment and Startup (0.5 day)
1. Keep PostgreSQL in Docker for reproducible local run and easy review.
2. Configure `application.yaml` to read DB settings from environment variables.
3. Make sure Flyway is enabled and points to the same database.

Why Docker DB is worth it:
- reviewer can run project quickly
- same DB version for everyone
- avoids "works on my machine" issues

Suggested `compose.yaml` direction:
```yaml
services:
  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: social
      POSTGRES_USER: social_user
      POSTGRES_PASSWORD: social_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

## 2) Database Design + Flyway (0.5 day)
Create migrations:
1. `V1__create_users_table.sql`
2. (optional) `V2__add_unique_login_constraint.sql` if not done in V1

Minimal schema:
- `id BIGSERIAL PRIMARY KEY`
- `login VARCHAR(50) NOT NULL UNIQUE`
- `password_hash VARCHAR(255) NOT NULL`
- `first_name VARCHAR(100) NOT NULL`
- `last_name VARCHAR(100) NOT NULL`
- `birth_date DATE NOT NULL`
- `gender VARCHAR(20) NOT NULL`
- `interests TEXT NOT NULL`
- `city VARCHAR(100) NOT NULL`
- `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`

Note: keep indexes minimal for this homework (PK + UNIQUE is enough).

## 3) Contracts and DTOs (0.5 day)
Define request/response models and validation.

Examples:
- `RegisterRequest(login, password, firstName, lastName, birthDate, gender, interests, city)`
- `LoginRequest(login, password)`
- `UserProfileResponse(id, firstName, lastName, birthDate, gender, interests, city)`
- `TokenResponse(token)`

Add bean validation (`@NotBlank`, `@Size`, `@NotNull`).

## 4) Repository Layer with JdbcTemplate (0.5 day)
Implement:
1. `createUser(...)`
2. `findByLogin(...)`
3. `findProfileById(...)`

`JdbcTemplate` patterns to use:
```java
// insert/update/delete
jdbcTemplate.update("UPDATE users SET city = ? WHERE id = ?", city, id);

// read list
List<UserEntity> rows = jdbcTemplate.query(sql, rowMapper, arg1);

// read optional single row
Optional<UserEntity> user = jdbcTemplate.query(sql, rowMapper, login).stream().findFirst();

// single scalar
Long id = jdbcTemplate.queryForObject(insertReturningSql, Long.class, p1, p2, p3);
```

Injection safety rule:
- only placeholders (`?`) + method args
- never concatenate user input into SQL strings

## 5) Service Layer and Security (0.5 day)
1. `UserService.register(...)`
2. `AuthService.login(...)`
3. `UserService.getProfileById(...)`

Security essentials:
- hash password with `BCryptPasswordEncoder`
- compare with `passwordEncoder.matches(raw, hash)`
- never return `password_hash` in API responses

Token approach for homework:
- simple JWT is enough
- return token from `/login`
- optionally protect `/user/get/{id}` by token (good plus)

## 6) Controllers + Error Handling (0.5 day)
Controllers:
- `POST /user/register` -> `201 Created` with new `id`
- `POST /login` -> `200 OK` with token
- `GET /user/get/{id}` -> `200 OK` with profile

Global errors via `@RestControllerAdvice`:
- `400` validation errors
- `401` wrong login/password
- `404` user not found
- `409` login already exists

## 7) Testing and Manual Verification (0.5 day)
Minimum checks:
1. Register user
2. Login with correct password
3. Login with wrong password -> `401`
4. Get existing profile by ID
5. Get non-existing profile -> `404`
6. Try SQL injection payload in login/ID fields -> should fail safely

## 8) Delivery Artifacts (0.5 day)
1. `README.md`:
- prerequisites
- `docker compose up -d`
- how to run app
- how Flyway migrations run
- sample `curl` calls

2. Postman collection:
- `Register`
- `Login`
- `Get User By Id`
- optional environment variables (`baseUrl`, `token`, `userId`)

3. Push source + Postman JSON to GitHub.

---

## Suggested Timeline (2-3 days part-time)
- Day 1: Phases 1-3
- Day 2: Phases 4-6
- Day 3: Phases 7-8 and polish

---

## Definition of Done
- Registration works
- Login works
- Get profile by ID works
- All SQL is parameterized
- Password is stored as hash
- Flyway migrations are reproducible
- README + Postman collection are included
