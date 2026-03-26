package ru.artem.highload.social.web.dto;

import jakarta.validation.constraints.Positive;

public record GenerateTestDataRequest(
        @Positive Integer count,
        @Positive Long maxAllowedData
) {
    private static final int DEFAULT_COUNT = 1_000_000;
    private static final long DEFAULT_MAX_ALLOWED_DATA = 1_000_000;

    public GenerateTestDataRequest {
        if (count == null) {
            count = DEFAULT_COUNT;
        }
        if (maxAllowedData == null) {
            maxAllowedData = DEFAULT_MAX_ALLOWED_DATA;
        }
    }
}
