package ru.artem.highload.social.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UserSearchRequest(
        @NotBlank String firstName,
        @NotBlank String lastName
) {
}
