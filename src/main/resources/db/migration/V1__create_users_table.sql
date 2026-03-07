CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    login      VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    birth_date DATE NOT NULL,
    gender     VARCHAR(20) NOT NULL,
    interests  TEXT NOT NULL,
    city       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO users (login, password_hash, first_name, last_name, birth_date, gender, interests, city)
VALUES (
    'ivan_petrov',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'Иван',
    'Петров',
    '1988-03-22',
    'MALE',
    'хоккей, рыбалка, программирование',
    'Москва'
);
