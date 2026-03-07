package ru.artem.highload.social.web.dto;

import java.time.LocalDate;

public record UserProfileResponse(
        Long id,
        String firstName,
        String lastName,
        LocalDate birthDate,
        String gender,
        String interests,
        String city
) {}
