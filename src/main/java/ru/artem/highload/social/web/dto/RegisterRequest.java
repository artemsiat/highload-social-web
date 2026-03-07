package ru.artem.highload.social.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterRequest(
        @NotBlank @Size(max = 50) String login,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull LocalDate birthDate,
        @NotBlank @Size(max = 20) String gender,
        @NotBlank String interests,
        @NotBlank @Size(max = 100) String city
) {}
