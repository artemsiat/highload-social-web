package ru.artem.highload.social.web.dto;

import jakarta.validation.constraints.Positive;

public record GenerateTestDataRequest(
        @Positive Integer count
) {
}
