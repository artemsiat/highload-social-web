package ru.artem.highload.social.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import ru.artem.highload.social.web.controller.TestDataController;
import ru.artem.highload.social.web.controller.UserController;
import ru.artem.highload.social.web.dto.GenerateTestDataRequest;
import ru.artem.highload.social.web.dto.GenerateTestDataResponse;
import ru.artem.highload.social.web.dto.UserProfileResponse;
import ru.artem.highload.social.web.dto.UserSearchRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
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
class UserSearchIntegrationTest {

    @Autowired
    private TestDataController testDataController;

    @Autowired
    private UserController userController;

    @Test
    void generateDataAndSearch() {
        // 1. Generate 100 test users with max 200
        GenerateTestDataResponse genResult = testDataController.generate(
                new GenerateTestDataRequest(100, 200L));

        assertThat(genResult.generatedCount()).isEqualTo(100);
        assertThat(genResult.message()).isEqualTo("Successfully generated 100 records");

        // 2. Search by prefix — results should be ordered by id
        List<UserProfileResponse> results = userController.search(new UserSearchRequest("Ив", "Пе"));
        assertThat(results).isNotNull();
        for (int i = 1; i < results.size(); i++) {
            assertThat(results.get(i).id()).isGreaterThan(results.get(i - 1).id());
        }

        // 3. Search with no matches
        List<UserProfileResponse> noMatch = userController.search(new UserSearchRequest("ZZZZZ", "YYYYY"));
        assertThat(noMatch).isEmpty();

        // 4. Generate more — capped by maxAllowedData (150 total, already 100, so only 50)
        GenerateTestDataResponse cappedResult = testDataController.generate(
                new GenerateTestDataRequest(200, 150L));

        assertThat(cappedResult.generatedCount()).isEqualTo(50);
        assertThat(cappedResult.totalRecords()).isEqualTo(150);

        // 5. Try to generate when already at max — should return 0
        GenerateTestDataResponse atMaxResult = testDataController.generate(
                new GenerateTestDataRequest(100, 150L));

        assertThat(atMaxResult.generatedCount()).isEqualTo(0);
    }
}
