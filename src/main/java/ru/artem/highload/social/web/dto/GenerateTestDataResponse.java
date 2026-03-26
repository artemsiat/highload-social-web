package ru.artem.highload.social.web.dto;

public record GenerateTestDataResponse(
        long generatedCount,
        long totalRecords,
        String message
) {
}
