package ru.artem.highload.social.web.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
public class UserEntity {
    private Long id;
    private String login;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String gender;
    private String interests;
    private String city;
    private OffsetDateTime createdAt;
}
