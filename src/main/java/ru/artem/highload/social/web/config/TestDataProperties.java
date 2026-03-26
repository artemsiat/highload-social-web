package ru.artem.highload.social.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.test-data")
public record TestDataProperties(
        long maxAllowedRecords,
        int defaultBatchSize
) {
}
