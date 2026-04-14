package ru.artem.highload.social.web.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ru.artem.highload.social.web.service.TestDataService;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestDataStartupRunner implements ApplicationRunner {

    private final TestDataProperties properties;
    private final TestDataService testDataService;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.generateOnStartup()) {
            log.info("Startup test data generation is disabled");
            return;
        }

        log.info("Startup test data generation enabled, target: {} records", properties.startupRecordCount());
        testDataService.generateOnStartup();
    }
}
