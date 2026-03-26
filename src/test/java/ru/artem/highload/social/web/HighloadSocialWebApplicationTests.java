package ru.artem.highload.social.web;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;INIT=CREATE SCHEMA IF NOT EXISTS highload_social_web",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.locations=classpath:db/migration/test",
        "spring.docker.compose.enabled=false",
        "app.test-data.max-allowed-records=10000000",
        "app.test-data.default-batch-size=1000000",
        "app.jwt.secret=test-secret-key-for-jwt-signing-must-be-at-least-256-bits-long",
        "app.jwt.expiration-ms=86400000"
})
class HighloadSocialWebApplicationTests {

    @Test
    void contextLoads() {
    }
}
